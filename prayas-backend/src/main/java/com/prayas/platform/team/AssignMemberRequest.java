package com.prayas.platform.team;

import jakarta.validation.constraints.NotNull;

/** Put {@code userId} on {@code domainId}'s team for a tour, as a coordinator or a volunteer. */
public record AssignMemberRequest(@NotNull Long domainId, @NotNull Long userId, @NotNull TeamRole role) {
}
