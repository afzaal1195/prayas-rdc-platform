package com.prayas.platform.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AuthorityRequest(
        @NotBlank(message = "Name is required.") @Size(max = 150, message = "Name is too long.") String name,
        @Size(max = 150, message = "Office is too long.") String office,
        @Size(max = 30, message = "Phone is too long.") String phone,
        @Email(message = "Enter a valid email address.") @Size(max = 255, message = "Email is too long.") String email,
        @Size(max = 150, message = "Office hours is too long.") String officeHours,
        @Size(max = 30, message = "Preferred contact is too long.") String preferredContact,
        @Size(max = 2000, message = "Notes are too long.") String notes,
        boolean active
) {
}
