package com.modelmate.leaderboard;

import com.modelmate.leaderboard.LeaderboardRepository.LeaderboardRow;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.IntStream;

@RestController
@RequestMapping("/api/v1/leaderboard")
@RequiredArgsConstructor
@Tag(name = "Leaderboard")
public class LeaderboardController {

    private final LeaderboardRepository leaderboard;

    public record LeaderboardEntry(
            int rank,
            boolean topThree,
            Long modelId,
            String name,
            String slug,
            String creator,
            String categorySlug,
            String categoryName,
            BigDecimal overall,
            long reviewCount
    ) {
    }

    @GetMapping
    @Transactional(readOnly = true)
    @Operation(summary = "Top-rated models, optionally filtered by category")
    public List<LeaderboardEntry> top(@RequestParam(required = false) String category,
                                      @RequestParam(defaultValue = "1") int minReviews) {
        String categorySlug = (category == null || category.isBlank()) ? null : category.trim();
        List<LeaderboardRow> rows = leaderboard.topModels(categorySlug, Math.max(minReviews, 1));
        return IntStream.range(0, rows.size())
                .mapToObj(i -> {
                    LeaderboardRow r = rows.get(i);
                    return new LeaderboardEntry(
                            i + 1,
                            i < 3,
                            r.getModelId(),
                            r.getModelName(),
                            r.getModelSlug(),
                            r.getCreator(),
                            r.getCategorySlug(),
                            r.getCategoryName(),
                            r.getOverall() == null ? null
                                    : BigDecimal.valueOf(r.getOverall()).setScale(2, RoundingMode.HALF_UP),
                            r.getReviewCount() == null ? 0 : r.getReviewCount());
                })
                .toList();
    }
}
