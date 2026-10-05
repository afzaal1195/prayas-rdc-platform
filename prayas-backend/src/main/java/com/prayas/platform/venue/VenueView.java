package com.prayas.platform.venue;

public record VenueView(Long id, String name, VenueType venueType, String department, String description) {
    public static VenueView from(Venue v) {
        return new VenueView(v.getId(), v.getName(), v.getVenueType(), v.getDepartment(), v.getDescription());
    }
}
