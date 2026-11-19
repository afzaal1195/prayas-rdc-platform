package com.prayas.platform.team;

import java.time.LocalDate;
import java.util.List;

/** The shapes the Teams page and My tours receive. */
public final class TeamViews {

    private TeamViews() {
    }

    public record TeamMember(Long assignmentId, Long userId, String name) {
    }

    public record UserOption(Long userId, String name) {
    }

    /**
     * One domain's team on one tour. The can* flags and option lists are
     * worked out for the person asking: someone who can't assign gets empty lists.
     */
    public record DomainTeam(
            Long domainId,
            String domainCode,
            String domainName,
            List<String> jobs,
            List<TeamMember> coordinators,
            List<TeamMember> volunteers,
            boolean canAssignCoordinator,
            boolean canAssignVolunteer,
            List<UserOption> coordinatorOptions,
            List<UserOption> volunteerOptions) {
    }

    public record TourTeams(Long programmeId, LocalDate visitDate, String schoolName, List<DomainTeam> domains) {
    }

    /** One of the signed-in person's own domain jobs. */
    public record DomainJob(
            Long assignmentId,
            Long programmeId,
            LocalDate visitDate,
            String schoolName,
            String domainName,
            TeamRole role,
            List<String> jobs,
            List<String> coordinatorNames) {
    }
}
