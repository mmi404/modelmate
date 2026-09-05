package com.modelmate.vote;

import com.modelmate.AbstractIntegrationTest;
import com.modelmate.discussion.Discussion;
import com.modelmate.discussion.DiscussionRepository;
import com.modelmate.security.AuthUser;
import com.modelmate.user.Role;
import com.modelmate.user.User;
import com.modelmate.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The vote counters used to be a read-modify-write on the entity, which loses
 * increments when people vote at the same time. The counter is now adjusted with
 * an atomic {@code count = count + delta} statement; this test pins that down.
 */
class VoteConcurrencyTest extends AbstractIntegrationTest {

    private static final int VOTERS = 12;

    @Autowired
    VoteService voteService;

    @Autowired
    UserRepository users;

    @Autowired
    DiscussionRepository discussions;

    private User newUser(String email) {
        User u = new User();
        u.setFirstName("Voter");
        u.setLastName(email.substring(0, 3));
        u.setEmail(email);
        u.setPasswordHash("!disabled");
        u.setRole(Role.USER);
        return users.save(u);
    }

    @Test
    void concurrentUpvotesAreAllCounted() throws Exception {
        User author = newUser("vc-author@example.com");
        Discussion discussion = new Discussion();
        discussion.setTitle("Concurrency");
        discussion.setContent("Do the counters hold up?");
        discussion.setAuthor(author);
        Long discussionId = discussions.save(discussion).getId();

        List<AuthUser> voters = new ArrayList<>();
        for (int i = 0; i < VOTERS; i++) {
            User u = newUser("vc-" + i + "@example.com");
            voters.add(new AuthUser(u.getId(), u.getEmail(), Role.USER));
        }

        ExecutorService pool = Executors.newFixedThreadPool(VOTERS);
        CountDownLatch startGun = new CountDownLatch(1);
        CountDownLatch finished = new CountDownLatch(VOTERS);
        AtomicInteger failures = new AtomicInteger();

        for (AuthUser voter : voters) {
            pool.submit(() -> {
                try {
                    startGun.await();
                    voteService.cast(voter, VoteTargetType.DISCUSSION, discussionId, 1);
                } catch (Exception e) {
                    failures.incrementAndGet();
                } finally {
                    finished.countDown();
                }
            });
        }

        startGun.countDown();
        assertThat(finished.await(60, TimeUnit.SECONDS)).isTrue();
        pool.shutdownNow();

        assertThat(failures.get()).isZero();
        Discussion reloaded = discussions.findById(discussionId).orElseThrow();
        assertThat(reloaded.getUpvoteCount()).isEqualTo(VOTERS);
        assertThat(reloaded.getDownvoteCount()).isZero();
    }

    @Test
    void switchingAndRemovingAVoteLeavesCountsBalanced() {
        User author = newUser("vs-author@example.com");
        Discussion discussion = new Discussion();
        discussion.setTitle("Switching");
        discussion.setContent("up then down then none");
        discussion.setAuthor(author);
        Long id = discussions.save(discussion).getId();

        User voterUser = newUser("vs-voter@example.com");
        AuthUser voter = new AuthUser(voterUser.getId(), voterUser.getEmail(), Role.USER);

        var afterUp = voteService.cast(voter, VoteTargetType.DISCUSSION, id, 1);
        assertThat(afterUp.upvoteCount()).isEqualTo(1);
        assertThat(afterUp.downvoteCount()).isZero();

        var afterDown = voteService.cast(voter, VoteTargetType.DISCUSSION, id, -1);
        assertThat(afterDown.upvoteCount()).isZero();
        assertThat(afterDown.downvoteCount()).isEqualTo(1);

        // Re-casting the same direction must not double-count.
        var again = voteService.cast(voter, VoteTargetType.DISCUSSION, id, -1);
        assertThat(again.downvoteCount()).isEqualTo(1);

        var afterRemove = voteService.remove(voter, VoteTargetType.DISCUSSION, id);
        assertThat(afterRemove.upvoteCount()).isZero();
        assertThat(afterRemove.downvoteCount()).isZero();
    }
}
