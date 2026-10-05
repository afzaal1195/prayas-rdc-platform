package com.prayas.platform.admin;

import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.user.GlobalRole;

import java.util.List;

public record AdminUserView(
        Long id,
        String email,
        String fullName,
        String phone,
        GlobalRole globalRole,
        boolean canFillVolunteerSlots,
        boolean active,
        List<MembershipView> memberships
) {
    public record MembershipView(String domainCode, String domainName, DomainRole role) {
    }
}
