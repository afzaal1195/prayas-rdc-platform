package com.prayas.platform.admin;

import com.prayas.platform.venue.VenueType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminVenueRequest(
        @NotBlank(message = "Venue name is required.") @Size(max = 150, message = "Venue name is too long.") String name,
        @NotNull(message = "Venue type is required.") VenueType venueType,
        @Size(max = 120, message = "Department is too long.") String department,
        @Min(value = 1, message = "Capacity must be at least 1.") Integer capacity,
        boolean requiresApproval,
        boolean publicVisible,
        boolean active,
        @Size(max = 2000, message = "Description is too long.") String description,
        Long authorityContactId
) {
}
