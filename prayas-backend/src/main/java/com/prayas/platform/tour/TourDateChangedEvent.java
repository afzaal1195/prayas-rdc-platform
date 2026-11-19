package com.prayas.platform.tour;

/**
 * Published (inside the same transaction) when staff approve a new date for a tour.
 * Anything that was planned around the old date, such as who is assigned to the tour, listens for it.
 */
public record TourDateChangedEvent(Long programmeId, Long actorUserId) {
}
