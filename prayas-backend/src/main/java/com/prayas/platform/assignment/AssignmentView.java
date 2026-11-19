package com.prayas.platform.assignment;

import java.time.Instant;

public record AssignmentView(
        Long id,
        Long userId,
        String userName,
        AssignmentRole role,
        Instant startsAt,
        Instant endsAt,
        AssignmentStatus status) {
}
