package com.modelmate.model;

import com.modelmate.common.PageResponse;
import com.modelmate.model.dto.ModelDtos.ModelCardDto;
import com.modelmate.model.dto.ModelDtos.ModelDetailDto;
import com.modelmate.model.dto.ModelDtos.ModelSummaryDto;
import com.modelmate.model.dto.ModelDtos.SubmissionAcceptedDto;
import com.modelmate.model.dto.ModelDtos.SubmitModelRequest;
import com.modelmate.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/models")
@RequiredArgsConstructor
@Tag(name = "Models")
public class ModelController {

    private final ModelService modelService;

    @GetMapping
    @Operation(summary = "List approved models with filtering, sorting and pagination")
    public PageResponse<ModelCardDto> list(@RequestParam(required = false) String category,
                                           @RequestParam(required = false) String q,
                                           @RequestParam(required = false) String sort,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return modelService.list(category, q, sort, pageable);
    }

    @GetMapping("/trending")
    @Operation(summary = "Models with the most recent review activity")
    public List<ModelCardDto> trending(@RequestParam(defaultValue = "12") int limit) {
        return modelService.trending(Math.min(Math.max(limit, 1), 50));
    }

    @GetMapping("/search")
    @Operation(summary = "Typeahead search over approved model names")
    public List<ModelSummaryDto> search(@RequestParam String q) {
        return modelService.search(q);
    }

    @GetMapping("/names")
    @Operation(summary = "All approved model names (for compare / review pickers)")
    public List<ModelSummaryDto> names() {
        return modelService.names();
    }

    @GetMapping("/compare")
    @Operation(summary = "Compare 2-3 models side by side")
    public List<ModelDetailDto> compare(@RequestParam String slugs) {
        return modelService.compare(slugs);
    }

    @GetMapping("/{slug}")
    @Operation(summary = "Full detail for one approved model")
    public ModelDetailDto get(@PathVariable String slug) {
        return modelService.getDetail(slug);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Submit a new model for review")
    public SubmissionAcceptedDto submit(@Valid @RequestBody SubmitModelRequest request,
                                        @AuthenticationPrincipal AuthUser principal) {
        return modelService.submit(request, principal);
    }
}
