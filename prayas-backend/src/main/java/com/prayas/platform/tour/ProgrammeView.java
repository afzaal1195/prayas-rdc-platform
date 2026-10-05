package com.prayas.platform.tour;

import java.time.LocalDate;

public record ProgrammeView(Long id, ProgrammeStatus status, LocalDate visitDate, LocalDate proposedDate) {
    public static ProgrammeView from(Programme p) {
        return new ProgrammeView(p.getId(), p.getStatus(), p.getVisitDate(), p.getProposedDate());
    }
}
