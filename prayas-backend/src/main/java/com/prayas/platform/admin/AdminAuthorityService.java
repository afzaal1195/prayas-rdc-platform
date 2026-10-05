package com.prayas.platform.admin;

import com.prayas.platform.authority.AuthorityContact;
import com.prayas.platform.authority.AuthorityContactRepository;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

/**
 * A contact can be deleted only while nothing points at it: no venue may
 * still name it as its approver. A contact that has been used is better
 * deactivated, so that the record of who was asked survives.
 */
@Service
public class AdminAuthorityService {

    private final AuthorityContactRepository authorities;
    private final VenueRepository venues;

    public AdminAuthorityService(AuthorityContactRepository authorities, VenueRepository venues) {
        this.authorities = authorities;
        this.venues = venues;
    }

    @Transactional(readOnly = true)
    public List<AuthorityView> list() {
        return authorities.findAllByOrderByNameAsc().stream().map(AuthorityView::from).toList();
    }

    @Transactional
    public AuthorityView create(AuthorityRequest request) {
        AuthorityContact authority = new AuthorityContact(request.name().trim());
        apply(authority, request);
        return AuthorityView.from(authorities.save(authority));
    }

    @Transactional
    public AuthorityView update(Long id, AuthorityRequest request) {
        AuthorityContact authority = authorities.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Authority contact not found."));
        apply(authority, request);
        return AuthorityView.from(authority);
    }

    @Transactional
    public void delete(Long id) {
        AuthorityContact authority = authorities.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Authority contact not found."));
        List<Venue> using = venues.findUsingAuthority(id);
        if (!using.isEmpty()) {
            String names = using.stream().limit(5).map(Venue::getName).collect(Collectors.joining(", "));
            String more = using.size() > 5 ? " and " + (using.size() - 5) + " more" : "";
            throw new IllegalStateException("\"" + authority.getName() + "\" is still the approver for: " + names + more
                    + ". Choose a different approver for those venues first, or mark this contact inactive instead.");
        }
        try {
            authorities.delete(authority);
            authorities.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("\"" + authority.getName() + "\" is still referenced by other records, "
                    + "so it can't be deleted. Mark it inactive instead.");
        }
    }

    private void apply(AuthorityContact a, AuthorityRequest r) {
        a.setName(r.name().trim());
        a.setOffice(blankToNull(r.office()));
        a.setPhone(blankToNull(r.phone()));
        a.setEmail(blankToNull(r.email()));
        a.setOfficeHours(blankToNull(r.officeHours()));
        a.setPreferredContact(blankToNull(r.preferredContact()));
        a.setNotes(blankToNull(r.notes()));
        a.setActive(r.active());
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
