package com.modelmate.model;

import com.modelmate.category.Category;
import com.modelmate.category.CategoryRepository;
import com.modelmate.common.PageResponse;
import com.modelmate.common.Slugs;
import com.modelmate.common.exception.NotFoundException;
import com.modelmate.model.dto.ModelDtos.CategoryRef;
import com.modelmate.model.dto.ModelDtos.ModelCardDto;
import com.modelmate.model.dto.ModelDtos.ModelDetailDto;
import com.modelmate.model.dto.ModelDtos.ModelSummaryDto;
import com.modelmate.model.dto.ModelDtos.RatingSummary;
import com.modelmate.model.dto.ModelDtos.SubmissionAcceptedDto;
import com.modelmate.model.dto.ModelDtos.SubmitModelRequest;
import com.modelmate.model.dto.ModelDtos.UserRef;
import com.modelmate.review.RatingAggregate;
import com.modelmate.review.ReviewRepository;
import com.modelmate.review.ReviewStatus;
import com.modelmate.review.ReviewType;
import com.modelmate.security.AuthUser;
import com.modelmate.user.User;
import com.modelmate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ModelService {

    private static final Set<String> SORTS = Set.of("newest", "name", "rating", "reviews");

    private final ModelRepository models;
    private final CategoryRepository categories;
    private final ReviewRepository reviews;
    private final UserRepository users;

    // ----- lists -----------------------------------------------------------

    public PageResponse<ModelCardDto> list(String categorySlug, String query, String sort, Pageable pageable) {
        Page<Model> page = models.searchApproved(
                blankToNull(categorySlug), blankToNull(query), normalizeSort(sort),
                PageRequest.of(pageable.getPageNumber(), pageable.getPageSize()));
        return PageResponse.of(toCards(page));
    }

    public List<ModelCardDto> trending(int limit) {
        List<Model> trending = models.findTrending(limit);
        Map<Long, RatingSummary> ratings = ratingsFor(ids(trending));
        Function<Long, CategoryRef> categoryRef = categoryRefLookup();
        return trending.stream()
                .map(m -> toCard(m, categoryRef, ratings.getOrDefault(m.getId(), RatingSummary.empty())))
                .toList();
    }

    public List<ModelSummaryDto> names() {
        return models.findByStatusOrderByCreatedAtDesc(ModelStatus.APPROVED).stream()
                .map(m -> new ModelSummaryDto(m.getId(), m.getName(), m.getSlug(), m.getCreator()))
                .toList();
    }

    public List<ModelSummaryDto> search(String query) {
        if (blankToNull(query) == null) {
            return List.of();
        }
        return models.searchApprovedByName(query.trim(), PageRequest.of(0, 10)).stream()
                .map(m -> new ModelSummaryDto(m.getId(), m.getName(), m.getSlug(), m.getCreator()))
                .toList();
    }

    // ----- detail ---------------------------------------------------------

    public ModelDetailDto getDetail(String slug) {
        Model model = models.findBySlug(slug)
                .filter(m -> m.getStatus() == ModelStatus.APPROVED)
                .orElseThrow(() -> NotFoundException.of("Model", slug));
        return toDetail(model);
    }

    public List<ModelDetailDto> compare(String slugsCsv) {
        List<String> slugs = Arrays.stream(slugsCsv.split(","))
                .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
        if (slugs.size() < 2 || slugs.size() > 3) {
            throw new IllegalArgumentException("Provide 2 or 3 model slugs to compare");
        }
        return slugs.stream().map(this::getDetail).toList();
    }

    // ----- submission ----------------------------------------------------

    @Transactional
    public SubmissionAcceptedDto submit(SubmitModelRequest request, AuthUser principal) {
        Category category = categories.findById(request.categoryId())
                .orElseThrow(() -> new IllegalArgumentException("Unknown category"));
        User submitter = users.getReferenceById(principal.id());

        Model model = new Model();
        model.setName(request.name().trim());
        model.setSlug(Slugs.unique(request.name(), models::existsBySlug));
        model.setCreator(blankToNull(request.creator()));
        model.setCategory(category);
        model.setDescription(blankToNull(request.description()));
        model.setWebsiteUrl(normalizeUrl(request.websiteUrl()));
        model.setStatus(ModelStatus.PENDING);
        model.setSubmittedBy(submitter);
        models.save(model);

        return new SubmissionAcceptedDto(model.getId(), "PENDING",
                "Your model was submitted and is pending review");
    }

    // ----- mapping helpers ----------------------------------------------

    private Page<ModelCardDto> toCards(Page<Model> page) {
        Map<Long, RatingSummary> ratings = ratingsFor(ids(page.getContent()));
        Function<Long, CategoryRef> categoryRef = categoryRefLookup();
        return page.map(m -> toCard(m, categoryRef,
                ratings.getOrDefault(m.getId(), RatingSummary.empty())));
    }

    private ModelCardDto toCard(Model m, Function<Long, CategoryRef> categoryRef, RatingSummary ratings) {
        return new ModelCardDto(m.getId(), m.getName(), m.getSlug(), m.getCreator(),
                categoryRef.apply(m.getCategory().getId()),
                m.getDescription(), ratings);
    }

    private ModelDetailDto toDetail(Model m) {
        RatingSummary ratings = ratingsFor(List.of(m.getId()))
                .getOrDefault(m.getId(), RatingSummary.empty());
        long problems = reviews.countByModelIdAndTypeAndStatus(
                m.getId(), ReviewType.PROBLEM, ReviewStatus.VISIBLE);
        User submitter = m.getSubmittedBy();
        return new ModelDetailDto(m.getId(), m.getName(), m.getSlug(), m.getCreator(),
                m.getDescription(), m.getWebsiteUrl(), m.getLogoUrl(),
                new CategoryRef(m.getCategory().getSlug(), m.getCategory().getName()),
                new UserRef(submitter.getId(), submitter.fullName()),
                m.getCreatedAt(), ratings, problems);
    }

    private Map<Long, RatingSummary> ratingsFor(Collection<Long> modelIds) {
        if (modelIds.isEmpty()) {
            return Map.of();
        }
        return reviews.aggregateForModels(modelIds).stream().collect(Collectors.toMap(
                RatingAggregate::getModelId,
                a -> new RatingSummary(
                        scale2(a.getOverall()),
                        round1(a.getAccuracy()), round1(a.getSpeed()), round1(a.getCost()),
                        round1(a.getEaseOfUse()), round1(a.getReliability()),
                        a.getReviewCount() == null ? 0 : a.getReviewCount())));
    }

    private Function<Long, CategoryRef> categoryRefLookup() {
        Map<Long, CategoryRef> byId = categories.findAll().stream()
                .collect(Collectors.toMap(Category::getId,
                        c -> new CategoryRef(c.getSlug(), c.getName())));
        return byId::get;
    }

    private static List<Long> ids(List<Model> list) {
        return list.stream().map(Model::getId).toList();
    }

    private static String normalizeSort(String sort) {
        if (sort == null || !SORTS.contains(sort.toLowerCase())) {
            return "newest";
        }
        return sort.toLowerCase();
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static String normalizeUrl(String url) {
        String trimmed = url.trim();
        if (!trimmed.matches("^https?://.*")) {
            return "https://" + trimmed;
        }
        return trimmed;
    }

    private static BigDecimal scale2(Double value) {
        return value == null ? null : BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }

    private static Double round1(Double value) {
        return value == null ? null
                : BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }
}
