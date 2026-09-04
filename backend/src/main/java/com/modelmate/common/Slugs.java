package com.modelmate.common;

import java.util.function.Predicate;

/**
 * Slug generation with collision handling.
 */
public final class Slugs {

    private Slugs() {
    }

    /**
     * Slugify {@code source}; if {@code exists} reports a clash, append {@code -2},
     * {@code -3}, ... until unique.
     */
    public static String unique(String source, Predicate<String> exists) {
        String base = SlugUtil.slugify(source);
        if (base.isEmpty()) {
            base = "item";
        }
        String candidate = base;
        int suffix = 2;
        while (exists.test(candidate)) {
            candidate = base + "-" + suffix++;
        }
        return candidate;
    }
}
