package com.modelmate.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelmate.common.ApiError;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-IP rate limiting, tiered by the kind of endpoint being hit (see {@link Tier}).
 * Runs inside the security chain, before authentication, so every key is the
 * caller's IP — never a client-supplied header, which would be trivially forgeable.
 *
 * <p>Bucket state is in-memory and therefore per-instance. That is adequate for the
 * single-instance deployment this app targets; running more than one replica would
 * multiply the effective limit by the replica count and needs a shared store.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    /** A group of endpoints that share one budget, with its own window and capacity. */
    private enum Tier {
        AUTH(5, Duration.ofMinutes(15)),
        WRITE(30, Duration.ofHours(1)),
        VOTE(30, Duration.ofMinutes(1)),
        SEARCH(60, Duration.ofMinutes(1));

        final int capacity;
        final Duration window;

        Tier(int capacity, Duration window) {
            this.capacity = capacity;
            this.window = window;
        }
    }

    /** Hard ceiling on tracked (tier|ip) keys — bounds worst-case memory under address rotation. */
    private static final int MAX_TRACKED_CLIENTS = 100_000;

    /** Sweep idle entries every N requests rather than on a timer thread. */
    private static final int SWEEP_EVERY_N_REQUESTS = 1_000;

    /** Longest tier window — an entry idle past this can never still be rate-limited. */
    private static final Duration MAX_WINDOW = Duration.ofHours(1);

    private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private final ObjectMapper objectMapper;

    private record Entry(Bucket bucket, AtomicLong lastSeenNanos) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return classify(request) == null;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        Tier tier = classify(request);
        if (tier == null) {
            filterChain.doFilter(request, response);
            return;
        }

        if (requestCounter.incrementAndGet() % SWEEP_EVERY_N_REQUESTS == 0) {
            evictIdleEntries();
        }

        // AUTH is keyed per path (5 logins AND 5 registrations, not 5 combined);
        // the other tiers share one budget across their whole group.
        String key = tier == Tier.AUTH
                ? "AUTH|" + request.getRequestURI() + "|" + request.getRemoteAddr()
                : tier.name() + "|" + request.getRemoteAddr();
        Entry entry = buckets.get(key);
        if (entry == null) {
            if (buckets.size() >= MAX_TRACKED_CLIENTS) {
                evictIdleEntries();
            }
            if (buckets.size() >= MAX_TRACKED_CLIENTS) {
                reject(request, response, tier); // fail closed rather than grow without bound
                return;
            }
            entry = buckets.computeIfAbsent(key,
                    k -> new Entry(newBucket(tier), new AtomicLong(System.nanoTime())));
        }
        entry.lastSeenNanos().set(System.nanoTime());

        if (entry.bucket().tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        reject(request, response, tier);
    }

    /** Which tier, if any, applies to this request. */
    private static Tier classify(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri.startsWith("/api/v1/auth/")
                && !uri.equals("/api/v1/auth/me") && !uri.equals("/api/v1/auth/logout")) {
            return Tier.AUTH;
        }
        if (uri.equals("/api/v1/models/search")) {
            return Tier.SEARCH;
        }
        if (uri.equals("/api/v1/votes")) {
            return Tier.VOTE;
        }
        boolean writeMethod = method.equals("POST") || method.equals("PUT") || method.equals("PATCH");
        if (writeMethod && (
                uri.equals("/api/v1/models")
                || uri.matches("/api/v1/models/\\d+/reviews")
                || uri.matches("/api/v1/reviews/\\d+")
                || uri.equals("/api/v1/discussions")
                || uri.matches("/api/v1/discussions/\\d+/replies")
                || uri.equals("/api/v1/me"))) {
            return Tier.WRITE;
        }
        return null;
    }

    private void reject(HttpServletRequest request, HttpServletResponse response, Tier tier) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(tier.window.toSeconds()));
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests",
                "Rate limit exceeded, try again later", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private void evictIdleEntries() {
        long cutoff = System.nanoTime() - MAX_WINDOW.toNanos();
        for (Iterator<Map.Entry<String, Entry>> it = buckets.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue().lastSeenNanos().get() < cutoff) {
                it.remove();
            }
        }
    }

    /** Test hook: drop all buckets so limits reset between test cases. */
    public void clearBuckets() {
        buckets.clear();
    }

    private static Bucket newBucket(Tier tier) {
        Bandwidth limit = Bandwidth.builder()
                .capacity(tier.capacity)
                .refillIntervally(tier.capacity, tier.window)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }
}
