package com.prayas.platform.itinerary;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/** The shapes the Itineraries page and the school's tracking page receive. */
public final class ItineraryViews {

    private ItineraryViews() {
    }

    public record Stop(
            Long id,
            Long venueId,
            String venueName,
            String title,
            LocalTime startTime,
            LocalTime endTime,
            String notes,
            StopReadiness readiness,
            String readinessNote,
            /** Things wrong with this stop right now (e.g. the tour's times moved); empty when fine. */
            List<String> problems) {
    }

    /** How prepared a whole tour is, worked out from its stops. */
    public enum Overall {
        NO_STOPS, NOT_READY, IN_PROGRESS, READY, BLOCKED
    }

    public record ReadinessSummary(int total, int ready, int blocked, Overall overall) {
    }

    public record TourReadiness(Long programmeId, ReadinessSummary summary) {
    }

    public record TourItinerary(
            Long programmeId,
            String schoolName,
            LocalDate visitDate,
            LocalTime windowStart,
            LocalTime windowEnd,
            ReadinessSummary readiness,
            List<Stop> stops) {
    }

    public record VenueOption(Long id, String name) {
    }

    /** The Itineraries page: every active venue (to pick from) and each upcoming approved tour's plan. */
    public record Board(List<VenueOption> venues, List<TourItinerary> tours) {
    }

    /** What the school sees: when and where, without internal notes or warnings. */
    public record PublicStop(String title, String venueName, LocalTime startTime, LocalTime endTime) {
    }
}
