package com.prayas.platform.approval;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Venue and mess approvals. Everything here needs AccessService.canWorkApprovals(). */
@RestController
@RequestMapping("/api/v1")
@PreAuthorize("@access.canWorkApprovals()")
public class ApprovalController {

    private final ApprovalService service;

    public ApprovalController(ApprovalService service) {
        this.service = service;
    }

    /** The team's queue: upcoming approved tours that still have a permission outstanding. */
    @GetMapping("/approvals")
    public List<ApprovalStepView> queue() {
        return service.openQueue();
    }

    @GetMapping("/tour-requests/{programmeId}/approvals")
    public List<ApprovalStepView> forTour(@PathVariable Long programmeId) {
        return service.forProgramme(programmeId);
    }

    @PatchMapping("/approvals/{stepId}")
    public ApprovalStepView update(@PathVariable Long stepId,
                                   @Valid @RequestBody ApprovalUpdateRequest request,
                                   @CurrentUser AppUser actor) {
        return service.update(stepId, request, actor);
    }
}
