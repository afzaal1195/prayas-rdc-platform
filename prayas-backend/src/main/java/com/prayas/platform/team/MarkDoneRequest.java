package com.prayas.platform.team;

import jakarta.validation.constraints.Size;

/** What a team member sends when they tick off their part of a tour. Both fields are optional. */
public record MarkDoneRequest(
        @Size(max = 500) String note,
        @Size(max = 500) String proofUrl) {
}
