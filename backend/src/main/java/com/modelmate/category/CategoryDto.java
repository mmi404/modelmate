package com.modelmate.category;

import java.util.Arrays;
import java.util.List;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        List<String> applications,
        long modelCount
) {
    public static CategoryDto from(Category category, long modelCount) {
        return new CategoryDto(category.getId(), category.getName(), category.getSlug(),
                category.getDescription(), splitApplications(category.getApplications()), modelCount);
    }

    static List<String> splitApplications(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(s -> s.trim().replaceAll("\\.$", "").trim())
                .filter(s -> !s.isEmpty())
                .toList();
    }
}
