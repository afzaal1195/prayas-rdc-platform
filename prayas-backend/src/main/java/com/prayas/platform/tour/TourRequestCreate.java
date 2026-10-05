package com.prayas.platform.tour;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record TourRequestCreate(
        @NotNull InstitutionType institutionType,
        @Valid @NotNull SchoolInfo school,
        @Valid @NotNull ContactInfo contact,
        @NotNull @Future LocalDate visitDate,
        LocalTime arrivalTime,
        LocalTime departureTime,
        // Only meaningful when institutionType == SCHOOL; validated conditionally
        // in PublicTourRequestService (must be non-empty there, optional here
        // since a single DTO covers both institution types).
        @Valid List<GradeCount> grades,
        // Only meaningful when institutionType == COLLEGE; same conditional rule.
        @Valid List<CourseCount> courses,
        @Valid @NotEmpty @Size(max = 20) List<TeacherInfo> teachers,
        @NotEmpty List<Long> venueIds,
        @Size(max = 2000) String interestNotes,
        @Valid @NotNull LunchInfo lunch,
        @Size(max = 30) String vehicleNumber,
        @NotBlank String captchaToken,
        // Honeypot: must stay empty. A bot filling every field trips this.
        String website
) {
    public record SchoolInfo(
            @NotBlank @Size(max = 200) String name,
            @NotBlank @Size(max = 500) String address,
            @NotBlank @Size(max = 120) String villageOrTown,
            @NotBlank @Size(max = 120) String district,
            @NotBlank @Size(max = 80) String state
    ) {
    }

    public record ContactInfo(
            @NotBlank @Size(max = 150) @Pattern(regexp = "^[A-Za-z .'-]+$",
                    message = "Name may only contain letters, spaces, and . ' -") String name,
            @NotBlank @Pattern(regexp = "^[0-9+ -]{7,20}$") String phone,
            @NotBlank @Email String email
    ) {
    }

    public record TeacherInfo(
            @NotBlank @Size(max = 150) @Pattern(regexp = "^[A-Za-z .'-]+$",
                    message = "Name may only contain letters, spaces, and . ' -") String name,
            @NotBlank @Pattern(regexp = "^[0-9+ -]{7,20}$") String phone
    ) {
    }

    /** One row of the school-side grade breakdown, e.g. Grade 8: 20 students. */
    public record GradeCount(
            @NotBlank @Pattern(regexp = "^([1-9]|1[0-2])$", message = "Grade must be 1-12") String grade,
            @Min(1) @Max(500) int count
    ) {
    }

    /** One row of the college-side course breakdown, e.g. B.Tech CSE, 2nd Year: 20 students. */
    public record CourseCount(
            @NotBlank @Size(max = 150) String courseOrBranch,
            @Size(max = 20) String yearOfStudy,
            @Min(1) @Max(500) int count
    ) {
    }

    /**
     * A single combined headcount for catering, not split by veg/non-veg on
     * the public form -- H&L can clarify the breakdown directly with the
     * school when confirming (they already make contact for venue
     * approvals). See PublicTourRequestService for how this maps onto the
     * existing veg_meals/non_veg_meals columns.
     */
    public record LunchInfo(
            boolean required,
            @Min(1) Integer count
    ) {
    }
}