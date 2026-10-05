package com.prayas.platform.admin;

import com.prayas.platform.authority.AuthorityContactRepository;
import com.prayas.platform.tour.TourInterestRepository;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import com.prayas.platform.venue.VenueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A venue that a tour request has picked is part of that request's history -- it must not be deletable. */
class AdminVenueServiceTest {

    private VenueRepository venues;
    private TourInterestRepository tourInterests;
    private AdminVenueService service;

    @BeforeEach
    void setUp() {
        venues = mock(VenueRepository.class);
        tourInterests = mock(TourInterestRepository.class);
        service = new AdminVenueService(venues, mock(AuthorityContactRepository.class), tourInterests);
    }

    @Test
    void cannotDeleteAVenueThatTourRequestsPicked() {
        Venue venue = new Venue("CSE Lab", VenueType.LAB);
        when(venues.findById(7L)).thenReturn(Optional.of(venue));
        when(tourInterests.countUsesOfVenue(7L)).thenReturn(2L);

        assertThatThrownBy(() -> service.delete(7L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CSE Lab")
                .hasMessageContaining("2 tour requests")
                .hasMessageContaining("inactive");
        verify(venues, never()).delete(venue);
    }

    @Test
    void deletesAVenueNothingRefersTo() {
        Venue venue = new Venue("Old Room", VenueType.CLASSROOM);
        when(venues.findById(8L)).thenReturn(Optional.of(venue));
        when(tourInterests.countUsesOfVenue(8L)).thenReturn(0L);

        service.delete(8L);

        verify(venues).delete(venue);
    }
}
