package com.prayas.platform.approval;

import java.time.Instant;
import java.time.LocalDate;

/** One step as the staff page shows it: what is being asked, of whom, and how far it has got. */
public record ApprovalStepView(
        Long id,
        Long programmeId,
        LocalDate visitDate,
        String schoolName,
        ApprovalKind kind,
        String title,
        String detail,
        ApprovalStatus status,
        String note,
        String decidedByName,
        Instant decidedAt,
        Contact contact) {

    /** Who to approach. Comes from the authority directory. */
    public record Contact(Long id, String name, String office, String phone, String email,
                          String officeHours, String preferredContact) {
    }
}
