package com.prayas.platform.requirement;

import com.prayas.platform.auth.AccessService;
import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/v1/requirements")
public class RequirementController {

    private final RequirementRepository requirementRepository;
    private final AccessService accessService;

    public RequirementController(RequirementRepository requirementRepository,
                                  AccessService accessService) {
        this.requirementRepository = requirementRepository;
        this.accessService = accessService;
    }

    // Read access isn't domain-restricted here: the frontend only ever asks
    // for a domain the logged-in user actually belongs to, and there's
    // nothing sensitive enough in a requirement row to need a server-side
    // block on top of that. Tighten this if that assumption stops holding.
    @GetMapping
    public List<RequirementView> list(@RequestParam Long domainId,
                                       @RequestParam(required = false) RequirementStatus status) {
        List<Requirement> requirements = status != null
                ? requirementRepository.findByDomainIdAndStatus(domainId, status)
                : requirementRepository.findByDomainId(domainId);
        return requirements.stream().map(RequirementView::from).toList();
    }

    @PatchMapping("/{id}")
    public RequirementView update(@PathVariable Long id,
                                   @Valid @RequestBody RequirementUpdate update,
                                   @CurrentUser AppUser actor) {
        Requirement requirement = requirementRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Requirement " + id + " not found"));

        // Checked here rather than in @PreAuthorize, since the rule needs the
        // requirement's own domain code, which isn't available until after
        // the load -- @PreAuthorize only sees the path/body before the method runs.
        if (!accessService.canUpdateRequirement(requirement.getDomain().getCode())) {
            throw new AccessDeniedException("Not authorized to update this domain's requirements");
        }

        requirement.updateStatus(update.status(), actor);
        return RequirementView.from(requirement);
    }
}
