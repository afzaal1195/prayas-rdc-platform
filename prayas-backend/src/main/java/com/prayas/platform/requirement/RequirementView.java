package com.prayas.platform.requirement;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record RequirementView(
        Long id,
        Long programmeId,
        String domainCode,
        RequirementKind kind,
        RequirementStatus status,
        boolean needsAttention,
        LocalDate dueDate
) {
    public static RequirementView from(Requirement r) {
        return new RequirementView(
                r.getId(), r.getProgrammeId(), r.getDomain().getCode(),
                r.getKind(), r.getStatus(), r.needsAttention(), r.getDueDate());
    }
}
