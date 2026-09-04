package com.modelmate.review;

/**
 * Aggregate rating projection for a single model, over its visible REVIEW rows.
 */
public interface RatingAggregate {

    Long getModelId();

    Double getOverall();

    Long getReviewCount();

    Double getAccuracy();

    Double getSpeed();

    Double getCost();

    Double getEaseOfUse();

    Double getReliability();
}
