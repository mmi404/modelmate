package com.modelmate.review;

import com.modelmate.model.Model;
import com.modelmate.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "reviews")
@Getter
@Setter
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "model_id", nullable = false)
    private Model model;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewType type;

    @Column(length = 255)
    private String title;

    @Column(columnDefinition = "text", nullable = false)
    private String content;

    private Short accuracy;
    private Short speed;
    private Short cost;

    @Column(name = "ease_of_use")
    private Short easeOfUse;

    private Short reliability;

    @Column(name = "overall_rating", precision = 3, scale = 2)
    private BigDecimal overallRating;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Severity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReviewStatus status = ReviewStatus.VISIBLE;

    @Column(name = "upvote_count", nullable = false)
    private int upvoteCount = 0;

    @Column(name = "downvote_count", nullable = false)
    private int downvoteCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    /**
     * Recomputes {@link #overallRating} as the mean of the five dimensions.
     * No-op when any dimension is null (e.g. a PROBLEM report).
     */
    public void recomputeOverall() {
        if (accuracy == null || speed == null || cost == null || easeOfUse == null || reliability == null) {
            this.overallRating = null;
            return;
        }
        int sum = accuracy + speed + cost + easeOfUse + reliability;
        this.overallRating = BigDecimal.valueOf(sum)
                .divide(BigDecimal.valueOf(5), 2, java.math.RoundingMode.HALF_UP);
    }
}
