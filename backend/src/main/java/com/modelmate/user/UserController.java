package com.modelmate.user;

import com.modelmate.common.PageResponse;
import com.modelmate.security.AuthUser;
import com.modelmate.user.dto.ProfileDtos.ContributionDto;
import com.modelmate.user.dto.ProfileDtos.UpdateProfileRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Users & Profile")
public class UserController {

    private final ProfileService profileService;

    @GetMapping("/api/v1/users/{id}")
    @Operation(summary = "Public profile")
    public UserDto profile(@PathVariable Long id) {
        return profileService.publicProfile(id);
    }

    @GetMapping("/api/v1/users/{id}/contributions")
    @Operation(summary = "A user's public contributions (reviews, problems, discussions, replies)")
    public PageResponse<ContributionDto> contributions(@PathVariable Long id,
                                                       @PageableDefault(size = 20) Pageable pageable) {
        return profileService.contributions(id, false, pageable);
    }

    @PutMapping("/api/v1/me")
    @Operation(summary = "Update your own profile")
    public UserDto updateMe(@Valid @RequestBody UpdateProfileRequest request,
                            @AuthenticationPrincipal AuthUser principal) {
        return profileService.updateMe(principal, request);
    }

    @GetMapping("/api/v1/me/contributions")
    @Operation(summary = "Your own contributions, including hidden ones")
    public PageResponse<ContributionDto> myContributions(@AuthenticationPrincipal AuthUser principal,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        return profileService.contributions(principal.id(), true, pageable);
    }
}
