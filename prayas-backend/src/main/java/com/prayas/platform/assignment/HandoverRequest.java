package com.prayas.platform.assignment;

import jakarta.validation.constraints.NotNull;
import java.time.LocalTime;

/** The person on the shift leaves at {@code at} (campus clock) and {@code newUserId}, of the same role, takes over until the end of the shift. */
public record HandoverRequest(@NotNull Long newUserId, @NotNull LocalTime at) {
}
