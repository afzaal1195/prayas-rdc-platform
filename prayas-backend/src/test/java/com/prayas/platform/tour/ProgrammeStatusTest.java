package com.prayas.platform.tour;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** The request lifecycle rules, in one place. */
class ProgrammeStatusTest {

    @Test
    void aRejectedRequestCanOnlyBeReopened() {
        assertThat(ProgrammeStatus.REJECTED.canMoveTo(ProgrammeStatus.UNDER_REVIEW)).isTrue();
        // ...never straight to approved, cancelled or anything else: it has to go back through review.
        for (ProgrammeStatus other : ProgrammeStatus.values()) {
            if (other != ProgrammeStatus.UNDER_REVIEW) {
                assertThat(ProgrammeStatus.REJECTED.canMoveTo(other)).isFalse();
            }
        }
    }

    @Test
    void cancelledAndCompletedRequestsAreStillFinal() {
        for (ProgrammeStatus other : ProgrammeStatus.values()) {
            assertThat(ProgrammeStatus.CANCELLED.canMoveTo(other)).isFalse();
            assertThat(ProgrammeStatus.COMPLETED.canMoveTo(other)).isFalse();
        }
        assertThat(ProgrammeStatus.CANCELLED.isFinal()).isTrue();
        assertThat(ProgrammeStatus.COMPLETED.isFinal()).isTrue();
    }

    @Test
    void theNormalReviewFlowIsUnchanged() {
        assertThat(ProgrammeStatus.SUBMITTED.canMoveTo(ProgrammeStatus.UNDER_REVIEW)).isTrue();
        assertThat(ProgrammeStatus.UNDER_REVIEW.canMoveTo(ProgrammeStatus.APPROVED)).isTrue();
        assertThat(ProgrammeStatus.UNDER_REVIEW.canMoveTo(ProgrammeStatus.REJECTED)).isTrue();
        assertThat(ProgrammeStatus.APPROVED.canMoveTo(ProgrammeStatus.COMPLETED)).isTrue();
        // approval is not undone by "reopening" -- an approved tour can only be cancelled or completed
        assertThat(ProgrammeStatus.APPROVED.canMoveTo(ProgrammeStatus.UNDER_REVIEW)).isFalse();
        assertThat(ProgrammeStatus.APPROVED.canMoveTo(ProgrammeStatus.REJECTED)).isFalse();
    }
}
