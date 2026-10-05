package com.prayas.platform.user;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "app_user")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "full_name", nullable = false)
    private String fullName;

    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "global_role", nullable = false)
    private GlobalRole globalRole = GlobalRole.MEMBER;

    @Column(name = "can_fill_volunteer_slots", nullable = false)
    private boolean canFillVolunteerSlots = false;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected AppUser() {
        // JPA
    }

    public AppUser(String email, String fullName) {
        this.email = email;
        this.fullName = fullName;
    }

    public boolean isLeadOrFaculty() {
        return globalRole == GlobalRole.LEAD || globalRole == GlobalRole.FACULTY_INCHARGE;
    }

    // --- getters / setters ---

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public GlobalRole getGlobalRole() {
        return globalRole;
    }

    public void setGlobalRole(GlobalRole globalRole) {
        this.globalRole = globalRole;
    }

    public boolean canFillVolunteerSlots() {
        return canFillVolunteerSlots;
    }

    public void setCanFillVolunteerSlots(boolean canFillVolunteerSlots) {
        this.canFillVolunteerSlots = canFillVolunteerSlots;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
