package com.prayas.platform.tour;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TourStatusView(
        ProgrammeStatus status,
        InstitutionType institutionType,
        LocalDate visitDate,
        LocalDate proposedDate, // set only when status == RESCHEDULE_PROPOSED
        LocalTime arrivalTime,
        LocalTime departureTime,
        int currentStudentCount,
        boolean lunchRequired,
        Integer mealCount,
        // Parsed back out of the stored grade_range display text so the
        // tracking page can pre-fill the current breakdown instead of
        // starting empty. Only one of the two is populated, matching
        // institutionType.
        List<TourRequestCreate.GradeCount> grades,
        List<TourRequestCreate.CourseCount> courses,
        // Set when staff rejected the request, or sometimes alongside an
        // approval/reschedule -- shown on the public tracking page so the
        // school knows why, e.g. "Slots full for this date".
        String decisionNote
) {
    public static TourStatusView from(Programme programme, CampusTourDetails details,
                                       int currentStudentCount, Integer mealCount,
                                       List<TourRequestCreate.GradeCount> grades,
                                       List<TourRequestCreate.CourseCount> courses) {
        return new TourStatusView(
                programme.getStatus(),
                details.getInstitutionType(),
                programme.getVisitDate(),
                programme.getProposedDate(),
                programme.getArrivalTime(),
                programme.getDepartureTime(),
                currentStudentCount,
                details.isLunchRequired(),
                mealCount,
                grades,
                courses,
                programme.getDecisionNote()
        );
    }
}