package com.prayas.platform.tour;

import java.time.LocalDate;
import java.time.LocalTime;

/** One row in the staff dashboard's tour-request list. */
public record ProgrammeSummaryView(
        Long id,
        String institutionName,
        String villageOrTown,
        String district,
        InstitutionType institutionType,
        LocalDate visitDate,
        LocalTime arrivalTime,
        LocalTime departureTime,
        ProgrammeStatus status,
        int studentCount,
        String contactName,
        String contactPhone,
        // Set while the school's request for a different date is waiting for staff.
        LocalDate requestedVisitDate,
        LocalTime requestedArrivalTime,
        LocalTime requestedDepartureTime
) {
}