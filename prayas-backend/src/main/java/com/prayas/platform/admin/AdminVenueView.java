package com.prayas.platform.admin;

import com.prayas.platform.authority.AuthorityContact;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueType;

public record AdminVenueView(
        Long id,
        String name,
        VenueType venueType,
        String department,
        Integer capacity,
        boolean requiresApproval,
        boolean publicVisible,
        boolean active,
        String description,
        Long authorityContactId,
        String authorityName
) {
    /** Reads the (lazy) authority contact, so call this inside a transaction. */
    public static AdminVenueView from(Venue v) {
        AuthorityContact authority = v.getAuthorityContact();
        return new AdminVenueView(
                v.getId(),
                v.getName(),
                v.getVenueType(),
                v.getDepartment(),
                v.getCapacity(),
                v.requiresApproval(),
                v.isPublicVisible(),
                v.isActive(),
                v.getDescription(),
                authority != null ? authority.getId() : null,
                authority != null ? authority.getName() : null
        );
    }
}
