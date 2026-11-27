package com.prayas.platform.auth;

import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

/**
 * Every authorization decision in the app goes through this class, so a
 * permission rule only has to be read (and tested) in one place, and the
 * frontend never has to be trusted to hide a button correctly.
 * Registered as bean "access" so @PreAuthorize("@access.canDecide(#id)") resolves.
 */
@Service("access")
public class AccessService {

    private static final String CAMPUS_TOUR = "CAMPUS_TOUR";

    private final DomainMembershipRepository membershipRepository;
    private final AppUserRepository appUserRepository;

    public AccessService(DomainMembershipRepository membershipRepository, AppUserRepository appUserRepository) {
        this.membershipRepository = membershipRepository;
        this.appUserRepository = appUserRepository;
    }

    /** Managing users, domain memberships, venues and the authority directory: Lead or Faculty In-charge only. */
    public boolean canAdmin() {
        AppUser user = currentUser();
        return user != null && user.isLeadOrFaculty();
    }

    /**
     * Seeing the list of tour requests (and their details): Lead, Faculty
     * In-charge, or a Campus Tour domain head/coordinator. Everyone else
     * works from their own queue instead -- a Hospitality coordinator sees
     * the lunch tasks, not every school's contact details.
     */
    public boolean canViewTourRequests() {
        AppUser user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.isLeadOrFaculty()) {
            return true;
        }
        return hasDomainRole(user, CAMPUS_TOUR, DomainRole.HEAD)
                || hasDomainRole(user, CAMPUS_TOUR, DomainRole.COORDINATOR);
    }

    /**
     * Planning an approved tour's itinerary: Lead, Faculty In-charge, or a Campus
     * Tour domain head or coordinator -- the people who run the tours themselves.
     */
    public boolean canPlanItineraries() {
        return canViewTourRequests();
    }

    /** Approving, rejecting or rescheduling: Lead, Faculty In-charge, or the Campus Tour domain HEAD. */
    public boolean canDecideTours() {
        AppUser user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.isLeadOrFaculty()) {
            return true;
        }
        return hasDomainRole(user, CAMPUS_TOUR, DomainRole.HEAD);
    }

    /**
     * Taking a rejection back so a request can be decided again: Lead or
     * Faculty In-charge only. Narrower than deciding -- a Campus Tour head can
     * reject, but undoing a rejection is a more senior call.
     */
    public boolean canReopenTours() {
        AppUser user = currentUser();
        return user != null && user.isLeadOrFaculty();
    }

    /** Lead, Faculty In-charge, or a Campus Tour domain HEAD may decide a tour request. */
    public boolean canDecide(Long programmeId) {
        return canDecideTours();
    }

    /** Domain coordinator/head, or Lead/Faculty, may update a requirement in that domain. */
    public boolean canUpdateRequirement(String domainCode) {
        AppUser user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.isLeadOrFaculty()) {
            return true;
        }
        return hasDomainRole(user, domainCode, DomainRole.HEAD)
                || hasDomainRole(user, domainCode, DomainRole.COORDINATOR);
    }

    /**
     * Putting coordinators and volunteers on approved tours: Lead, Faculty
     * In-charge, or a head/coordinator of the Volunteer Coordination Unit or
     * Campus Tour domain.
     */
    public boolean canAssign() {
        AppUser user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.isLeadOrFaculty()) {
            return true;
        }
        return hasDomainRole(user, "VCU", DomainRole.HEAD)
                || hasDomainRole(user, "VCU", DomainRole.COORDINATOR)
                || hasDomainRole(user, CAMPUS_TOUR, DomainRole.HEAD)
                || hasDomainRole(user, CAMPUS_TOUR, DomainRole.COORDINATOR);
    }

    /**
     * Seeing and recording venue and mess approvals: Lead, Faculty In-charge,
     * or anyone in Hospitality & Logistics (head, coordinator or volunteer --
     * the volunteers are the ones who go and ask), or a Campus Tour head or coordinator.
     */
    public boolean canWorkApprovals() {
        AppUser user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.isLeadOrFaculty()) {
            return true;
        }
        for (DomainRole role : DomainRole.values()) {
            if (hasDomainRole(user, "HOSPITALITY_LOGISTICS", role)) {
                return true;
            }
        }
        return hasDomainRole(user, CAMPUS_TOUR, DomainRole.HEAD)
                || hasDomainRole(user, CAMPUS_TOUR, DomainRole.COORDINATOR);
    }

    /**
     * Seeing the Teams page: Lead, Faculty In-charge, or a head or coordinator
     * of any domain. What each person can change there is decided by the
     * chain of command in DomainTeamService.
     */
    public boolean canWorkTeams() {
        AppUser user = currentUser();
        if (user == null) {
            return false;
        }
        if (user.isLeadOrFaculty()) {
            return true;
        }
        return membershipRepository.findByUserId(user.getId()).stream()
                .anyMatch(m -> m.getDomainRole() == DomainRole.HEAD || m.getDomainRole() == DomainRole.COORDINATOR);
    }

    private boolean hasDomainRole(AppUser user, String domainCode, DomainRole role) {
        return membershipRepository.existsByUserAndDomainCodeAndRole(user.getId(), domainCode, role);
    }

    private AppUser currentUser() {
        Object principal = SecurityContextHolder.getContext().getAuthentication() == null
                ? null
                : SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof OAuth2User oauthUser)) {
            return null;
        }
        String email = oauthUser.getAttribute("email");
        if (email == null) {
            return null;
        }
        return appUserRepository.findByEmailIgnoreCase(email)
                .filter(AppUser::isActive)
                .orElse(null);
    }
}
