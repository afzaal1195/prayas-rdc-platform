package com.prayas.platform.assignment;

import java.time.Instant;
import java.time.LocalDate;

/** One shift as the person doing it sees it: which tour, when, and who to meet. */
public record MyAssignmentView(
        Long assignmentId,
        Long programmeId,
        AssignmentRole role,
        Instant startsAt,
        Instant endsAt,
        LocalDate visitDate,
        String institutionName,
        String villageOrTown,
        String district,
        int studentCount,
        String contactName,
        String contactPhone) {
}
