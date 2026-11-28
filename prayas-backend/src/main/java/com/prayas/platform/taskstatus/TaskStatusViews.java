package com.prayas.platform.taskstatus;

import com.prayas.platform.requirement.RequirementKind;
import com.prayas.platform.requirement.RequirementStatus;

import com.prayas.platform.team.TeamRole;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** The shapes the Tasks page receives. */
public final class TaskStatusViews {

    private TaskStatusViews() {
    }

    /** One person on a domain's team for a tour, and whether they have ticked off their part. */
    public record MemberStatus(
            String name,
            TeamRole role,
            boolean done,
            Instant completedAt,
            String note,
            String proofUrl) {
    }

    /** One task a domain owes one tour. */
    public record TaskRow(
            Long requirementId,
            Long programmeId,
            String schoolName,
            LocalDate visitDate,
            RequirementKind kind,
            String title,
            RequirementStatus status,
            boolean needsAttention,
            LocalDate dueDate,
            /** e.g. "1 of 2 approvals done"; null when the task has no steps (volunteers). */
            String progress,
            List<String> coordinators,
            List<String> volunteers,
            /** True when the signed-in person may set the status by hand (not for tasks worked out from approval steps). */
            boolean canSetStatus,
            /** Everyone on the domain's team for this tour, with whether each has finished their part. */
            List<MemberStatus> members) {
    }

    /** How many of a domain's tasks are in each state. */
    public record Counts(int total, int pending, int inProgress, int done, int blocked, int needsAttention) {
    }

    public record DomainStatus(
            Long domainId,
            String domainCode,
            String domainName,
            Counts counts,
            List<TaskRow> tasks) {
    }

    /** {@code overview} is true for the faculty in-charge and leads, who see every domain. */
    public record TaskBoard(boolean overview, Counts totals, List<DomainStatus> domains) {
    }
}
