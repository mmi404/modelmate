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
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Per-IP rate limit on the unauthenticated auth endpoints: 5 requests / 15 minutes.
 * Runs inside the security chain, before authentication.
 *
 * <p>Bucket state is in-memory and therefore per-instance. That is adequate for the
 * single-instance deployment this app targets; running more than one replica would
 * multiply the effective limit by the replica count and needs a shared store.
 */
@Component
@RequiredArgsConstructor
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Set<String> LIMITED_PATHS = Set.of(
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/verify-reset-code",
            "/api/v1/auth/reset-password");

    private static final Duration WINDOW = Duration.ofMinutes(15);
    private static final int CAPACITY = 5;

    /**
     * Hard ceiling on tracked clients. Buckets are only evicted once a client has
     * been idle for a full window, so this also bounds worst-case memory if an
     * attacker rotates source addresses.
     */
    private static final int MAX_TRACKED_CLIENTS = 50_000;

    /** Sweep idle entries every N requests rather than on a timer thread. */
    private static final int SWEEP_EVERY_N_REQUESTS = 1_000;

    private final ConcurrentHashMap<String, Entry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong requestCounter = new AtomicLong();
    private final ObjectMapper objectMapper;

    private record Entry(Bucket bucket, AtomicLong lastSeenNanos) {
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !LIMITED_PATHS.contains(request.getRequestURI());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        if (requestCounter.incrementAndGet() % SWEEP_EVERY_N_REQUESTS == 0) {
            evictIdleEntries();
        }

        String key = clientKey(request);
        Entry entry = buckets.get(key);
        if (entry == null) {
            if (buckets.size() >= MAX_TRACKED_CLIENTS) {
                evictIdleEntries();
            }
            if (buckets.size() >= MAX_TRACKED_CLIENTS) {
                // Fail closed: refuse rather than grow without bound.
                reject(request, response);
                return;
            }
            entry = buckets.computeIfAbsent(key, k -> new Entry(newBucket(), new AtomicLong(System.nanoTime())));
        }
        entry.lastSeenNanos().set(System.nanoTime());

        if (entry.bucket().tryConsume(1)) {
            filterChain.doFilter(request, response);
            return;
        }
        reject(request, response);
    }

    private void reject(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setHeader("Retry-After", String.valueOf(WINDOW.toSeconds()));
        ApiError body = ApiError.of(HttpStatus.TOO_MANY_REQUESTS.value(), "Too Many Requests",
                "Rate limit exceeded, try again later", request.getRequestURI());
        objectMapper.writeValue(response.getOutputStream(), body);
    }

    private void evictIdleEntries() {
        long cutoff = System.nanoTime() - WINDOW.toNanos();
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

    private Bucket newBucket() {
        Bandwidth limit = Bandwidth.builder()
                .capacity(CAPACITY)
                .refillIntervally(CAPACITY, WINDOW)
                .build();
        return Bucket.builder().addLimit(limit).build();
    }

    /**
     * Uses the container-resolved remote address only. Spring's
     * {@code server.forward-headers-strategy=framework} already rewrites this from
     * the proxy's X-Forwarded-For, so reading that header here as well would let any
     * client set its own rate-limit key and bypass the limit entirely.
     */
    private String clientKey(HttpServletRequest request) {
        return request.getRequestURI() + "|" + request.getRemoteAddr();
    }
}
