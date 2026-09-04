package com.modelmate.vote;

/**
 * Something users can up/down-vote. Implemented by Discussion, Reply and Review
 * so {@link VoteService} can adjust their denormalised counters uniformly.
 */
public interface Votable {

    int getUpvoteCount();

    void setUpvoteCount(int count);

    int getDownvoteCount();

    void setDownvoteCount(int count);
}
