package com.prayas.platform.assignment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** from/to are optional campus-clock times; leave both out to cover the whole visit. */
public record AssignRequest(
        @NotNull Long userId,
        @NotNull AssignmentRole role,
        LocalTime from,
        LocalTime to) {
}
