package com.prayas.platform.tour;

import java.util.EnumSet;
import java.util.Set;

/**
 * Programme lifecycle. Keeping the transition rules here (rather than
 * scattered through services) means the whole state machine is covered
 * by one focused unit test.
 *
 * REJECTED is not quite final: a rejection can be taken back (REJECTED ->
 * UNDER_REVIEW) so the request can be decided again. That is deliberately the
 * ONLY way out of REJECTED -- a rejected request can't jump straight to
 * approved or cancelled -- and who may do it is decided elsewhere
 * (AccessService.canReopenTours), not by this rule.
 */
public enum ProgrammeStatus {
    SUBMITTED,
    UNDER_REVIEW,
    RESCHEDULE_PROPOSED,
    APPROVED,
    REJECTED,
    CANCELLED,
    COMPLETED;

    // Nothing ever leaves these.
    private static final Set<ProgrammeStatus> FINAL_STATES =
            EnumSet.of(CANCELLED, COMPLETED);

    public boolean isFinal() {
        return FINAL_STATES.contains(this);
    }

    public boolean canMoveTo(ProgrammeStatus next) {
        if (this.isFinal()) {
            return false;
        }
        return switch (this) {
            case SUBMITTED -> next == UNDER_REVIEW || next == CANCELLED;
            case UNDER_REVIEW -> next == APPROVED || next == REJECTED
                    || next == RESCHEDULE_PROPOSED || next == CANCELLED;
            case RESCHEDULE_PROPOSED -> next == UNDER_REVIEW || next == CANCELLED;
            case APPROVED -> next == COMPLETED || next == CANCELLED;
            case REJECTED -> next == UNDER_REVIEW;
            default -> false;
        };
    }
}
