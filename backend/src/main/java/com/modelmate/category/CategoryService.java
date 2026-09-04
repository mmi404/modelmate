package com.modelmate.category;

import com.modelmate.common.exception.NotFoundException;
import com.modelmate.model.ModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categories;
    private final ModelRepository models;

    public List<CategoryDto> listAll() {
        Map<Long, Long> counts = models.countApprovedByCategory().stream()
                .collect(Collectors.toMap(ModelRepository.CategoryCount::getCategoryId,
                        ModelRepository.CategoryCount::getCount));
        return categories.findAll(Sort.by("name")).stream()
                .map(c -> CategoryDto.from(c, counts.getOrDefault(c.getId(), 0L)))
                .toList();
    }

    public CategoryDto getBySlug(String slug) {
        Category category = categories.findBySlug(slug)
                .orElseThrow(() -> NotFoundException.of("Category", slug));
        long count = models.countApprovedByCategory().stream()
                .filter(c -> c.getCategoryId().equals(category.getId()))
                .map(ModelRepository.CategoryCount::getCount)
                .findFirst().orElse(0L);
        return CategoryDto.from(category, count);
    }

    public Category requireBySlug(String slug) {
        return categories.findBySlug(slug).orElseThrow(() -> NotFoundException.of("Category", slug));
    }
}
