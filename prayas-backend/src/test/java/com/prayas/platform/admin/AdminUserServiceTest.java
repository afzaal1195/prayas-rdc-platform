package com.prayas.platform.admin;

import com.prayas.platform.auth.EmailDomainPolicy;
import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.domain.DomainRepository;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import com.prayas.platform.user.GlobalRole;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * The rules that stop the admin screen from locking everyone out of it, and
 * from creating accounts that could never sign in. Pure unit tests -- no
 * database, no Spring.
 */
class AdminUserServiceTest {

    private AppUserRepository users;
    private DomainRepository domains;
    private DomainMembershipRepository memberships;
    private AdminUserService service;

    @BeforeEach
    void setUp() {
        users = mock(AppUserRepository.class);
        domains = mock(DomainRepository.class);
        memberships = mock(DomainMembershipRepository.class);
        service = new AdminUserService(users, domains, memberships, new EmailDomainPolicy("iith.ac.in"));
    }

    private AppUser existingUser(long id, String email, GlobalRole role) {
        AppUser user = new AppUser(email, "Existing Person");
        user.setGlobalRole(role);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private AdminUserRequest request(String email, GlobalRole role, boolean active,
                                     List<AdminUserRequest.MembershipInput> memberships) {
        return new AdminUserRequest(email, "Some Name", null, role, false, active, memberships);
    }

    @Test
    void cannotChangeYourOwnRole() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.FACULTY_INCHARGE);
        when(users.findById(1L)).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> service.update(1L,
                request("me@iith.ac.in", GlobalRole.MEMBER, true, List.of()), me))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own role");
        verify(memberships, never()).deleteAllForUser(1L);
    }

    @Test
    void cannotDeactivateYourOwnAccount() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.LEAD);
        when(users.findById(1L)).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> service.update(1L,
                request("me@iith.ac.in", GlobalRole.LEAD, false, List.of()), me))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("deactivate");
        assertThat(me.isActive()).isTrue();
    }

    @Test
    void canChangeAnotherPersonsRole() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.FACULTY_INCHARGE);
        AppUser other = existingUser(2L, "other@iith.ac.in", GlobalRole.MEMBER);
        when(users.findById(2L)).thenReturn(Optional.of(other));

        AdminUserView view = service.update(2L,
                request("other@iith.ac.in", GlobalRole.LEAD, true, List.of()), me);

        assertThat(view.globalRole()).isEqualTo(GlobalRole.LEAD);
        verify(memberships).deleteAllForUser(2L);
    }

    @Test
    void cannotChangeAnEmailAddress() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.FACULTY_INCHARGE);
        AppUser other = existingUser(2L, "other@iith.ac.in", GlobalRole.MEMBER);
        when(users.findById(2L)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.update(2L,
                request("different@iith.ac.in", GlobalRole.MEMBER, true, List.of()), me))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @Test
    void createRejectsAnAddressSignInWouldRefuse() {
        assertThatThrownBy(() -> service.create(
                request("someone@gmail.com", GlobalRole.MEMBER, true, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("@iith.ac.in");
        verify(users, never()).save(org.mockito.ArgumentMatchers.any(AppUser.class));
    }

    @Test
    void createRejectsALookalikeDomain() {
        assertThatThrownBy(() -> service.create(
                request("someone@notiith.ac.in", GlobalRole.MEMBER, true, List.of())))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createRejectsADuplicateEmail() {
        when(users.findByEmailIgnoreCase("dup@iith.ac.in"))
                .thenReturn(Optional.of(existingUser(5L, "dup@iith.ac.in", GlobalRole.MEMBER)));

        assertThatThrownBy(() -> service.create(
                request("dup@iith.ac.in", GlobalRole.MEMBER, true, List.of())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void aPersonBelongsToOneDomainOnly() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.FACULTY_INCHARGE);
        AppUser other = existingUser(2L, "other@iith.ac.in", GlobalRole.MEMBER);
        when(users.findById(2L)).thenReturn(Optional.of(other));
        List<AdminUserRequest.MembershipInput> two = List.of(
                new AdminUserRequest.MembershipInput("VCU", com.prayas.platform.domain.DomainRole.COORDINATOR),
                new AdminUserRequest.MembershipInput("CAMPUS_TOUR", com.prayas.platform.domain.DomainRole.HEAD));

        assertThatThrownBy(() -> service.update(2L,
                request("other@iith.ac.in", GlobalRole.MEMBER, true, two), me))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("one domain");
        verify(memberships, never()).deleteAllForUser(2L);
    }

    @Test
    void cannotDeleteYourOwnAccount() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.FACULTY_INCHARGE);
        when(users.findById(1L)).thenReturn(Optional.of(me));

        assertThatThrownBy(() -> service.delete(1L, me))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("own account");
        verify(users, never()).delete(me);
    }

    @Test
    void deletesAnotherPersonAndTheirDomainRole() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.FACULTY_INCHARGE);
        AppUser other = existingUser(2L, "mistake@iith.ac.in", GlobalRole.MEMBER);
        when(users.findById(2L)).thenReturn(Optional.of(other));

        service.delete(2L, me);

        verify(memberships).deleteAllForUser(2L);
        verify(users).delete(other);
    }

    @Test
    void explainsWhyAPersonWithRecordedActivityCannotBeDeleted() {
        AppUser me = existingUser(1L, "me@iith.ac.in", GlobalRole.FACULTY_INCHARGE);
        AppUser decider = existingUser(3L, "decider@iith.ac.in", GlobalRole.LEAD);
        when(users.findById(3L)).thenReturn(Optional.of(decider));
        // the database refuses: this person is referenced by requests they decided
        doThrow(new DataIntegrityViolationException("fk_programme_decided_by")).when(users).flush();

        assertThatThrownBy(() -> service.delete(3L, me))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inactive");
    }
}
