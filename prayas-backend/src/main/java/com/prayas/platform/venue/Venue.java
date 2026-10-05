package com.prayas.platform.venue;

import com.prayas.platform.authority.AuthorityContact;
import jakarta.persistence.*;

@Entity
@Table(name = "venue")
public class Venue {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "venue_type", nullable = false)
    private VenueType venueType;

    private String department;

    private Integer capacity;

    // Set manually by an admin per venue -- there is no blanket rule.
    // When true, approving a tour that includes this venue spawns a
    // VENUE_APPROVALS requirement for Hospitality & Logistics (phase 2).
    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval = false;

    @Column(name = "public_visible", nullable = false)
    private boolean publicVisible = true;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    // Who has to say yes before a school can be shown this venue. Optional:
    // a venue that doesn't need approval doesn't need a contact either.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "authority_contact_id")
    private AuthorityContact authorityContact;

    protected Venue() {
        // JPA
    }

    public Venue(String name, VenueType venueType) {
        this.name = name;
        this.venueType = venueType;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public VenueType getVenueType() {
        return venueType;
    }

    public void setVenueType(VenueType venueType) {
        this.venueType = venueType;
    }

    public boolean requiresApproval() {
        return requiresApproval;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public boolean isPublicVisible() {
        return publicVisible;
    }

    public void setPublicVisible(boolean publicVisible) {
        this.publicVisible = publicVisible;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public AuthorityContact getAuthorityContact() {
        return authorityContact;
    }

    public void setAuthorityContact(AuthorityContact authorityContact) {
        this.authorityContact = authorityContact;
    }
}
