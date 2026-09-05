package com.modelmate.common.web;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.ShallowEtagHeaderFilter;

/**
 * Adds a content-hash {@code ETag} to GET responses so clients (and Cloudflare)
 * can revalidate with {@code If-None-Match} and get a cheap {@code 304}. The
 * public read API is the main beneficiary — see docs/SEO-GEO.md.
 */
@Configuration
public class WebCachingConfig {

    @Bean
    public FilterRegistrationBean<ShallowEtagHeaderFilter> shallowEtagHeaderFilter() {
        FilterRegistrationBean<ShallowEtagHeaderFilter> reg =
                new FilterRegistrationBean<>(new ShallowEtagHeaderFilter());
        reg.addUrlPatterns("/api/v1/categories/*", "/api/v1/models/*", "/api/v1/discussions/*",
                "/api/v1/leaderboard/*", "/api/v1/users/*", "/api/v1/reviews/recent");
        reg.setName("shallowEtagHeaderFilter");
        return reg;
    }
}
