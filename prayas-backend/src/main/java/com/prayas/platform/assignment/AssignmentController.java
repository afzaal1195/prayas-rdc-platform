package com.prayas.platform.assignment;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalTime;
import java.util.List;

/** Who is doing what on an approved tour. Everything here needs AccessService.canAssign(). */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("@access.canAssign()")
public class AssignmentController {

    private final AssignmentService service;

    public AssignmentController(AssignmentService service) {
        this.service = service;
    }

    @GetMapping("/tour-requests/{programmeId}/assignments")
    public List<AssignmentView> list(@PathVariable Long programmeId) {
        return service.list(programmeId);
    }

    /** People who could take a shift on this tour right now. from/to narrow it to part of the visit (HH:mm). */
    @GetMapping("/tour-requests/{programmeId}/assignable")
    public List<AssigneeOption> assignable(@PathVariable Long programmeId,
                                           @RequestParam AssignmentRole role,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime from,
                                           @RequestParam(required = false)
                                           @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime to) {
        return service.available(programmeId, role, from, to);
    }

    @PostMapping("/tour-requests/{programmeId}/assignments")
    @ResponseStatus(HttpStatus.CREATED)
    public AssignmentView assign(@PathVariable Long programmeId,
                                 @Valid @RequestBody AssignRequest request,
                                 @CurrentUser AppUser actor) {
        return service.assign(programmeId, request, actor);
    }

    @PostMapping("/assignments/{assignmentId}/handover")
    public List<AssignmentView> handover(@PathVariable Long assignmentId,
                                         @Valid @RequestBody HandoverRequest request,
                                         @CurrentUser AppUser actor) {
        return service.handover(assignmentId, request, actor);
    }

    @DeleteMapping("/assignments/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long assignmentId, @CurrentUser AppUser actor) {
        service.release(assignmentId, actor);
    }
}
