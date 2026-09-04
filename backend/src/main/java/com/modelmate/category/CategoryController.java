package com.modelmate.category;

import com.modelmate.common.PageResponse;
import com.modelmate.model.ModelService;
import com.modelmate.model.dto.ModelDtos.ModelCardDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final ModelService modelService;

    @GetMapping
    @Operation(summary = "List all categories with their approved-model counts")
    public List<CategoryDto> list() {
        return categoryService.listAll();
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Get a single category by slug")
    public CategoryDto get(@PathVariable String slug) {
        return categoryService.getBySlug(slug);
    }

    @GetMapping("/{slug}/models")
    @Operation(summary = "List approved models in a category")
    public PageResponse<ModelCardDto> models(@PathVariable String slug,
                                             @RequestParam(required = false) String sort,
                                             @PageableDefault(size = 20) Pageable pageable) {
        categoryService.requireBySlug(slug);
        return modelService.list(slug, null, sort, pageable);
    }
}
