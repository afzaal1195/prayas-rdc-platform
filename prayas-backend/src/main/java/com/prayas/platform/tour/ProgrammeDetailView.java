package com.prayas.platform.tour;

import java.util.List;

/** The extra detail shown when a staff dashboard row is expanded -- everything the summary row doesn't already show. */
public record ProgrammeDetailView(
        String address,
        String state,
        String contactEmail,
        String gradeRange,
        int teacherCount,
        List<TeacherInfo> teachers,
        boolean lunchRequired,
        Integer mealCount,
        String vehicleNumber,
        String interestNotes,
        String decisionNote
) {
    public record TeacherInfo(String name, String phone) {
    }
}