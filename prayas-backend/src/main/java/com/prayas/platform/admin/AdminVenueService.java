package com.prayas.platform.admin;

import com.prayas.platform.authority.AuthorityContact;
import com.prayas.platform.authority.AuthorityContactRepository;
import com.prayas.platform.tour.TourInterestRepository;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class AdminVenueService {

    private final VenueRepository venues;
    private final AuthorityContactRepository authorities;
    private final TourInterestRepository tourInterests;

    public AdminVenueService(VenueRepository venues, AuthorityContactRepository authorities,
                             TourInterestRepository tourInterests) {
        this.venues = venues;
        this.authorities = authorities;
        this.tourInterests = tourInterests;
    }

    @Transactional(readOnly = true)
    public List<AdminVenueView> list() {
        return venues.findAllByOrderByNameAsc().stream().map(AdminVenueView::from).toList();
    }

    @Transactional
    public AdminVenueView create(AdminVenueRequest request) {
        Venue venue = new Venue(request.name().trim(), request.venueType());
        apply(venue, request);
        return AdminVenueView.from(venues.save(venue));
    }

    @Transactional
    public AdminVenueView update(Long id, AdminVenueRequest request) {
        Venue venue = venues.findById(id).orElseThrow(() -> new NoSuchElementException("Venue not found."));
        apply(venue, request);
        return AdminVenueView.from(venue);
    }

    /**
     * A venue that any tour request has picked is part of that request's
     * history, so it can't be deleted -- mark it inactive instead (schools
     * can no longer choose it, and the history stays).
     */
    @Transactional
    public void delete(Long id) {
        Venue venue = venues.findById(id).orElseThrow(() -> new NoSuchElementException("Venue not found."));
        long uses = tourInterests.countUsesOfVenue(id);
        if (uses > 0) {
            throw new IllegalStateException("\"" + venue.getName() + "\" was chosen in " + uses
                    + (uses == 1 ? " tour request" : " tour requests")
                    + ", so it can't be deleted without erasing that history. Mark it inactive instead -- "
                    + "schools can no longer pick it, and the history stays.");
        }
        try {
            venues.delete(venue);
            venues.flush();
        } catch (DataIntegrityViolationException e) {
            throw new IllegalStateException("\"" + venue.getName() + "\" is still referenced by other records, "
                    + "so it can't be deleted. Mark it inactive instead.");
        }
    }

    private void apply(Venue venue, AdminVenueRequest r) {
        venue.setName(r.name().trim());
        venue.setVenueType(r.venueType());
        venue.setDepartment(blankToNull(r.department()));
        venue.setCapacity(r.capacity());
        venue.setRequiresApproval(r.requiresApproval());
        venue.setPublicVisible(r.publicVisible());
        venue.setActive(r.active());
        venue.setDescription(blankToNull(r.description()));
        if (r.authorityContactId() == null) {
            venue.setAuthorityContact(null);
        } else {
            AuthorityContact authority = authorities.findById(r.authorityContactId())
                    .orElseThrow(() -> new IllegalArgumentException("That authority contact no longer exists."));
            venue.setAuthorityContact(authority);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
