package com.prayas.platform.tour;

public record TourRequestCreated(
        Long programmeId,
        String trackingToken, // shown once; the school must save this link
        ProgrammeStatus status
) {
}
