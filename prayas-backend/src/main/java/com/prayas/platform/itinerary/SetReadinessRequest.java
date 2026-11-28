package com.prayas.platform.itinerary;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Mark a stop ready, not ready, or blocked. A blocked stop needs a note saying why. */
public record SetReadinessRequest(
        @NotNull StopReadiness readiness,
        @Size(max = 500, message = "That note is too long.") String note) {
}
