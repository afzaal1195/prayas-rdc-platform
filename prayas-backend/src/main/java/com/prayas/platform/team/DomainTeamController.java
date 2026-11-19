package com.prayas.platform.team;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.team.TeamViews.DomainJob;
import com.prayas.platform.team.TeamViews.TourTeams;
import com.prayas.platform.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * The chain of command. The Teams page needs AccessService.canWorkTeams();
 * the rules about who may assign whom are in DomainTeamService. "My domain
 * jobs" only ever returns the caller's own, so it needs no special permission.
 */
@RestController
@RequestMapping("/api/v1")
public class DomainTeamController {

    private final DomainTeamService service;

    public DomainTeamController(DomainTeamService service) {
        this.service = service;
    }

    @GetMapping("/teams")
    @PreAuthorize("@access.canWorkTeams()")
    public List<TourTeams> teams(@CurrentUser AppUser actor) {
        return service.teams(actor);
    }

    @PostMapping("/tour-requests/{programmeId}/team")
    @PreAuthorize("@access.canWorkTeams()")
    @ResponseStatus(HttpStatus.CREATED)
    public void assign(@PathVariable Long programmeId,
                       @Valid @RequestBody AssignMemberRequest request,
                       @CurrentUser AppUser actor) {
        service.assign(programmeId, request, actor);
    }

    @DeleteMapping("/team/{assignmentId}")
    @PreAuthorize("@access.canWorkTeams()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable Long assignmentId, @CurrentUser AppUser actor) {
        service.remove(assignmentId, actor);
    }

    @GetMapping("/me/domain-jobs")
    public List<DomainJob> mine(@CurrentUser AppUser user) {
        return service.myJobs(user.getId());
    }
}
