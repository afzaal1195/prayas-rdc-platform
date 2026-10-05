package com.prayas.platform.tour;

import com.prayas.platform.school.School;
import jakarta.persistence.*;

@Entity
@Table(name = "campus_tour_details")
public class CampusTourDetails {

    @Id
    @Column(name = "programme_id")
    private Long programmeId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "programme_id")
    private Programme programme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "school_id", nullable = false)
    private School school;

    @Enumerated(EnumType.STRING)
    @Column(name = "institution_type", nullable = false)
    private InstitutionType institutionType = InstitutionType.SCHOOL;

    @Column(name = "contact_name", nullable = false)
    private String contactName;

    @Column(name = "contact_phone", nullable = false)
    private String contactPhone;

    @Column(name = "contact_email", nullable = false)
    private String contactEmail;

    // School: e.g. "8-10". College: formatted from courseOrBranch + yearOfStudy, e.g. "B.Tech CSE, 2nd Year".
    @Column(name = "grade_range")
    private String gradeRange;

    @Column(name = "vehicle_number")
    private String vehicleNumber;

    @Column(name = "arrival_point")
    private String arrivalPoint;

    @Column(name = "lunch_required", nullable = false)
    private boolean lunchRequired = false;

    @Column(name = "interest_notes", columnDefinition = "text")
    private String interestNotes;

    @Column(name = "special_needs", columnDefinition = "text")
    private String specialNeeds;

    // SHA-256 hex digest of the tracking token. The raw token is shown to the
    // school exactly once (response body + confirmation email) and never stored.
    @Column(name = "tracking_token_hash", nullable = false, unique = true, length = 64)
    private String trackingTokenHash;

    protected CampusTourDetails() {
        // JPA
    }

    public CampusTourDetails(Programme programme, School school, String contactName,
                              String contactPhone, String trackingTokenHash) {
        this.programme = programme;
        this.school = school;
        this.contactName = contactName;
        this.contactPhone = contactPhone;
        this.trackingTokenHash = trackingTokenHash;
    }

    public Programme getProgramme() {
        return programme;
    }

    public School getSchool() {
        return school;
    }

    public String getContactName() {
        return contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public InstitutionType getInstitutionType() {
        return institutionType;
    }

    public void setInstitutionType(InstitutionType institutionType) {
        this.institutionType = institutionType;
    }

    public boolean isLunchRequired() {
        return lunchRequired;
    }

    public void setLunchRequired(boolean lunchRequired) {
        this.lunchRequired = lunchRequired;
    }

    public String getContactEmail() {
        return contactEmail;
    }

    public void setContactEmail(String contactEmail) {
        this.contactEmail = contactEmail;
    }

    public String getGradeRange() {
        return gradeRange;
    }

    public void setGradeRange(String gradeRange) {
        this.gradeRange = gradeRange;
    }

    public String getVehicleNumber() {
        return vehicleNumber;
    }

    public void setVehicleNumber(String vehicleNumber) {
        this.vehicleNumber = vehicleNumber;
    }

    public String getInterestNotes() {
        return interestNotes;
    }

    public void setInterestNotes(String interestNotes) {
        this.interestNotes = interestNotes;
    }

        public String getTrackingTokenHash() {
        return trackingTokenHash;
    }

    /**
     * Staff issuing a fresh tracking link when a school has lost theirs.
     * The raw token is never stored (see TrackingTokenGenerator), so a lost
     * link can never be recovered -- only replaced. This intentionally
     * invalidates whatever link was issued before.
     */
    public void regenerateTrackingTokenHash(String newHash) {
        this.trackingTokenHash = newHash;
    }
}