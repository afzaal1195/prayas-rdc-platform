package com.prayas.platform.approval;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Recording progress on a step. A denial needs a note (say why), so the next
 * person knows what to fix. {@code authorityContactId} optionally changes who is being approached.
 */
public record ApprovalUpdateRequest(
        @NotNull ApprovalStatus status,
        @Size(max = 500, message = "That note is too long.") String note,
        Long authorityContactId) {
}
