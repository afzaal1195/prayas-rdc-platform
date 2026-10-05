package com.prayas.platform.admin;

import com.prayas.platform.authority.AuthorityContact;

public record AuthorityView(
        Long id,
        String name,
        String office,
        String phone,
        String email,
        String officeHours,
        String preferredContact,
        String notes,
        boolean active
) {
    public static AuthorityView from(AuthorityContact a) {
        return new AuthorityView(a.getId(), a.getName(), a.getOffice(), a.getPhone(), a.getEmail(),
                a.getOfficeHours(), a.getPreferredContact(), a.getNotes(), a.isActive());
    }
}
