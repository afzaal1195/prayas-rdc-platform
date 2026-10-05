package com.prayas.platform.admin;

import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.user.GlobalRole;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Used for both creating and editing a staff member. On edit the email must
 * match the existing one (it is the account's identity); it is carried here
 * only so create and edit share one shape.
 */
public record AdminUserRequest(
        @NotBlank(message = "Email is required.")
        @Email(message = "Enter a valid email address.")
        @Size(max = 255, message = "Email is too long.") String email,
        @NotBlank(message = "Name is required.") @Size(max = 150, message = "Name is too long.") String fullName,
        @Size(max = 20, message = "Phone is too long.") String phone,
        @NotNull(message = "Role is required.") GlobalRole globalRole,
        boolean canFillVolunteerSlots,
        boolean active,
        @NotNull(message = "Memberships are required (send an empty list for none).")
        @Valid List<MembershipInput> memberships
) {
    public record MembershipInput(
            @NotBlank(message = "Domain is required.") String domainCode,
            @NotNull(message = "Domain role is required.") DomainRole role
    ) {
    }
}
