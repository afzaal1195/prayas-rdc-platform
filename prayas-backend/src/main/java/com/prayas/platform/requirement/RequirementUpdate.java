package com.prayas.platform.requirement;

import jakarta.validation.constraints.NotNull;

public record RequirementUpdate(@NotNull RequirementStatus status) {
}
