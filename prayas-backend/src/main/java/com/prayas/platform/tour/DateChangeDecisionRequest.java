package com.prayas.platform.tour;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Staff answering a school's request for a different date. The note is optional and shown to the school when declining. */
public record DateChangeDecisionRequest(
        @NotNull Decision decision,
        @Size(max = 500, message = "That note is too long.") String note) {

    public enum Decision {
        APPROVE,
        DECLINE
    }
}
