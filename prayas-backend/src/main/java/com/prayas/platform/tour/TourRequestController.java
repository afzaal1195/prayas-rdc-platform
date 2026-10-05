package com.prayas.platform.tour;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.user.AppUser;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/tour-requests")
public class TourRequestController {

    private final TourDecisionService decisionService;

    public TourRequestController(TourDecisionService decisionService) {
        this.decisionService = decisionService;
    }

    // Viewing is limited to Lead / Faculty In-charge / Campus Tour head or
    // coordinator (AccessService.canViewTourRequests); deciding is narrower
    // still (canDecide). Other staff work from their own task queues.
    @GetMapping
    @PreAuthorize("@access.canViewTourRequests()")
    public List<ProgrammeSummaryView> list(@CurrentUser AppUser ignoredButRequiresLogin) {
        return decisionService.listAll();
    }

    // Access is enforced by AccessService.canDecide, which checks the caller
    // is a Lead, Faculty In-charge, or a Campus Tour domain head.
    @PostMapping("/{id}/decision")
    @PreAuthorize("@access.canDecide(#id)")
    public ProgrammeView decide(@PathVariable Long id,
                                 @Valid @RequestBody DecisionRequest request,
                                 @CurrentUser AppUser actor) {
        Programme programme = decisionService.decide(id, request, actor);
        return ProgrammeView.from(programme);
    }

    public record ReopenRequest(
            @NotBlank(message = "Please say why this request is being reopened.")
            @Size(max = 500, message = "That reason is too long.") String reason) {
    }

    // Only a Lead / Faculty In-charge may take a rejection back (AccessService.canReopenTours).
    @PostMapping("/{id}/reopen")
    @PreAuthorize("@access.canReopenTours()")
    public ProgrammeView reopen(@PathVariable Long id,
                                @Valid @RequestBody ReopenRequest request,
                                @CurrentUser AppUser actor) {
        return ProgrammeView.from(decisionService.reopen(id, request.reason().trim(), actor));
    }

    @GetMapping("/{id}/detail")
    @PreAuthorize("@access.canViewTourRequests()")
    public ProgrammeDetailView detail(@PathVariable Long id, @CurrentUser AppUser ignoredButRequiresLogin) {
        return decisionService.detail(id);
    }

    // Anyone who can see the request may issue a fresh link for a school that
    // lost theirs -- see TourDecisionService.regenerateTrackingLink for why
    // the old one can never simply be looked back up.
    @PostMapping("/{id}/tracking-link")
    @PreAuthorize("@access.canViewTourRequests()")
    public Map<String, String> regenerateTrackingLink(@PathVariable Long id,
                                                        @CurrentUser AppUser ignoredButRequiresLogin) {
        String rawToken = decisionService.regenerateTrackingLink(id);
        return Map.of("trackingToken", rawToken);
    }
}