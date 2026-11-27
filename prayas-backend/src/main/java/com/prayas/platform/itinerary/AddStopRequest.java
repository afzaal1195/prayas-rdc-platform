package com.prayas.platform.itinerary;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalTime;

/**
 * Add a stop. Give a {@code venueId} (the title then defaults to the venue's name) or just a
 * {@code title} for something that isn't a venue, such as a welcome talk or lunch.
 */
public record AddStopRequest(
        Long venueId,
        @Size(max = 150, message = "That title is too long.") String title,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime,
        @Size(max = 500, message = "Those notes are too long.") String notes) {
}
