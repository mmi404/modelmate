package com.modelmate.admin;

import com.modelmate.admin.dto.AdminDtos.AdminStats;
import com.modelmate.admin.dto.AdminDtos.HideReviewRequest;
import com.modelmate.admin.dto.AdminDtos.ModerationResultDto;
import com.modelmate.admin.dto.AdminDtos.PendingModelDto;
import com.modelmate.admin.dto.AdminDtos.RejectModelRequest;
import com.modelmate.common.PageResponse;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/models/pending")
    @Operation(summary = "List models awaiting review (oldest first)")
    public PageResponse<PendingModelDto> pendingModels(@PageableDefault(size = 20) Pageable pageable) {
        return adminService.pendingModels(pageable);
    }

    @PostMapping("/models/{id}/approve")
    @Operation(summary = "Approve a pending model")
    public ModerationResultDto approve(@PathVariable Long id, @AuthenticationPrincipal AuthUser admin) {
        return adminService.approve(id, admin);
    }

    @PostMapping("/models/{id}/reject")
    @Operation(summary = "Reject a pending model")
    public ModerationResultDto reject(@PathVariable Long id, @Valid @RequestBody RejectModelRequest request) {
        return adminService.reject(id, request.reason());
    }

    @PatchMapping("/reviews/{id}/hidden")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Hide or unhide a review / problem report")
    public void setReviewHidden(@PathVariable Long id, @Valid @RequestBody HideReviewRequest request) {
        adminService.setReviewHidden(id, request.hidden());
    }

    @GetMapping("/stats")
    @Operation(summary = "Dashboard counts")
    public AdminStats stats() {
        return adminService.stats();
    }
}
