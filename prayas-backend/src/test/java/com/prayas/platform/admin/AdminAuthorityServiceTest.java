package com.prayas.platform.admin;

import com.prayas.platform.authority.AuthorityContact;
import com.prayas.platform.authority.AuthorityContactRepository;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import com.prayas.platform.venue.VenueType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** A contact that venues still rely on as their approver must not silently disappear. */
class AdminAuthorityServiceTest {

    private AuthorityContactRepository authorities;
    private VenueRepository venues;
    private AdminAuthorityService service;

    @BeforeEach
    void setUp() {
        authorities = mock(AuthorityContactRepository.class);
        venues = mock(VenueRepository.class);
        service = new AdminAuthorityService(authorities, venues);
    }

    @Test
    void cannotDeleteAContactThatVenuesStillUse() {
        AuthorityContact contact = new AuthorityContact("HoD, CSE");
        when(authorities.findById(3L)).thenReturn(Optional.of(contact));
        when(venues.findUsingAuthority(3L)).thenReturn(List.of(new Venue("CSE Lab", VenueType.LAB)));

        assertThatThrownBy(() -> service.delete(3L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("CSE Lab")
                .hasMessageContaining("approver");
        verify(authorities, never()).delete(contact);
    }

    @Test
    void deletesAContactNothingRefersTo() {
        AuthorityContact contact = new AuthorityContact("Old contact");
        when(authorities.findById(4L)).thenReturn(Optional.of(contact));
        when(venues.findUsingAuthority(4L)).thenReturn(List.of());

        service.delete(4L);

        verify(authorities).delete(contact);
    }
}
