package com.prayas.platform.admin;

import com.prayas.platform.auth.EmailDomainPolicy;
import com.prayas.platform.domain.Domain;
import com.prayas.platform.domain.DomainMembership;
import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.domain.DomainRepository;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Staff accounts and their domain roles. A person belongs to ONE domain at
 * most (with one role in it: head, coordinator or volunteer).
 *
 * Rules that stop the admin screen from locking everyone out of it:
 *  - you cannot change your own global role, deactivate yourself, or delete
 *    yourself, so the last lead/faculty member can't remove their own access;
 *  - an account can only be created for an address that Google sign-in
 *    would actually accept (see EmailDomainPolicy).
 */
@Service
public class AdminUserService {

    private final AppUserRepository users;
    private final DomainRepository domains;
    private final DomainMembershipRepository memberships;
    private final EmailDomainPolicy emailPolicy;

    public AdminUserService(AppUserRepository users, DomainRepository domains,
                            DomainMembershipRepository memberships, EmailDomainPolicy emailPolicy) {
        this.users = users;
        this.domains = domains;
        this.memberships = memberships;
        this.emailPolicy = emailPolicy;
    }

    @Transactional(readOnly = true)
    public List<AdminUserView> list() {
        Map<Long, List<AdminUserView.MembershipView>> byUser = new HashMap<>();
        for (DomainMembership m : memberships.findAllWithDomainAndUser()) {
            byUser.computeIfAbsent(m.getUser().getId(), id -> new ArrayList<>()).add(toMembershipView(m));
        }
        return users.findAll(Sort.by("fullName")).stream()
                .map(u -> toView(u, byUser.getOrDefault(u.getId(), List.of())))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<DomainView> listDomains() {
        return domains.findAll(Sort.by("name")).stream()
                .map(d -> new DomainView(d.getCode(), d.getName()))
                .toList();
    }

    @Transactional
    public AdminUserView create(AdminUserRequest request) {
        String email = request.email().trim();
        if (!emailPolicy.isAllowed(email)) {
            throw new IllegalArgumentException("Staff accounts must use an @" + emailPolicy.allowedDomain()
                    + " email address, since that's the only kind that can sign in.");
        }
        if (users.findByEmailIgnoreCase(email).isPresent()) {
            throw new IllegalStateException("A user with this email already exists.");
        }
        AppUser user = new AppUser(email, request.fullName().trim());
        applyProfile(user, request);
        users.save(user);
        replaceMemberships(user.getId(), request.memberships());
        return toView(user, loadMemberships(user.getId()));
    }

    @Transactional
    public AdminUserView update(Long id, AdminUserRequest request, AppUser actor) {
        AppUser user = users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found."));
        if (!user.getEmail().equalsIgnoreCase(request.email().trim())) {
            throw new IllegalArgumentException("A user's email can't be changed -- add a new user instead.");
        }
        if (user.getId().equals(actor.getId())) {
            if (request.globalRole() != user.getGlobalRole()) {
                throw new IllegalStateException(
                        "You can't change your own role -- ask another lead or faculty member to do it.");
            }
            if (!request.active()) {
                throw new IllegalStateException("You can't deactivate your own account.");
            }
        }
        user.setFullName(request.fullName().trim());
        applyProfile(user, request);
        replaceMemberships(user.getId(), request.memberships());
        return toView(user, loadMemberships(user.getId()));
    }

    /**
     * Deleting is only possible for someone nothing else refers to -- an
     * account added by mistake, say. Anyone who has decided a request,
     * changed a headcount or left an audit entry is part of the history, so
     * they are deactivated instead (which also stops them signing in).
     */
    @Transactional
    public void delete(Long id, AppUser actor) {
        AppUser user = users.findById(id).orElseThrow(() -> new NoSuchElementException("User not found."));
        if (user.getId().equals(actor.getId())) {
            throw new IllegalStateException("You can't delete your own account.");
        }
        memberships.deleteAllForUser(id);
        try {
            users.delete(user);
            users.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("This person has recorded activity (for example requests they decided), "
                    + "so they can't be deleted without erasing that history. Mark them inactive instead -- "
                    + "that stops them signing in and keeps the history intact.");
        }
    }

    private void applyProfile(AppUser user, AdminUserRequest r) {
        user.setPhone(r.phone() == null || r.phone().isBlank() ? null : r.phone().trim());
        user.setGlobalRole(r.globalRole());
        user.setCanFillVolunteerSlots(r.canFillVolunteerSlots());
        user.setActive(r.active());
    }

    /**
     * Replaces the person's domain role with the requested one (or clears it
     * when none is requested). A person belongs to one domain only.
     */
    private void replaceMemberships(Long userId, List<AdminUserRequest.MembershipInput> requested) {
        if (requested.size() > 1) {
            throw new IllegalArgumentException(
                    "A person belongs to one domain only -- choose a single domain and role.");
        }
        // Resolve the domain before touching anything, so a typo fails cleanly
        // instead of after the old role has been removed.
        Domain domain = null;
        if (!requested.isEmpty()) {
            String code = requested.get(0).domainCode();
            domain = domains.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Unknown domain: " + code));
        }
        memberships.deleteAllForUser(userId);
        if (domain != null) {
            memberships.addMembership(userId, domain.getId(), requested.get(0).role().name());
        }
    }

    private List<AdminUserView.MembershipView> loadMemberships(Long userId) {
        return memberships.findByUserId(userId).stream().map(this::toMembershipView).toList();
    }

    private AdminUserView.MembershipView toMembershipView(DomainMembership m) {
        return new AdminUserView.MembershipView(m.getDomain().getCode(), m.getDomain().getName(), m.getDomainRole());
    }

    private AdminUserView toView(AppUser u, List<AdminUserView.MembershipView> membershipViews) {
        return new AdminUserView(u.getId(), u.getEmail(), u.getFullName(), u.getPhone(), u.getGlobalRole(),
                u.canFillVolunteerSlots(), u.isActive(), membershipViews);
    }
}
