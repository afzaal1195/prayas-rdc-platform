package com.prayas.platform.itinerary;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalTime;

/** One stop on an approved tour: a venue (or a free-text activity) between two times of the visit day. */
@Entity
@Table(name = "itinerary_stop")
public class ItineraryStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "programme_id", nullable = false)
    private Long programmeId;

    @Column(name = "venue_id")
    private Long venueId;

    @Column(nullable = false)
    private String title;

    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Column(columnDefinition = "text")
    private String notes;

    @Column(name = "created_by")
    private Long createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StopReadiness readiness = StopReadiness.NOT_READY;

    @Column(name = "readiness_note", columnDefinition = "text")
    private String readinessNote;

    @Column(name = "readiness_updated_by")
    private Long readinessUpdatedBy;

    @Column(name = "readiness_updated_at")
    private Instant readinessUpdatedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ItineraryStop() {
        // JPA
    }

    public ItineraryStop(Long programmeId, Long venueId, String title, LocalTime startTime, LocalTime endTime,
                         String notes, Long createdBy) {
        this.programmeId = programmeId;
        this.venueId = venueId;
        this.title = title;
        this.startTime = startTime;
        this.endTime = endTime;
        this.notes = notes;
        this.createdBy = createdBy;
    }

    /** Records who set the stop's readiness and when. The note is kept only while it explains something. */
    public void markReadiness(StopReadiness readiness, String note, Long userId) {
        this.readiness = readiness;
        this.readinessNote = note;
        this.readinessUpdatedBy = userId;
        this.readinessUpdatedAt = Instant.now();
    }

    /** Back to not ready: used when the tour moved to a new date, because the preparation was for the old one. */
    public void resetReadiness() {
        this.readiness = StopReadiness.NOT_READY;
        this.readinessNote = null;
        this.readinessUpdatedBy = null;
        this.readinessUpdatedAt = null;
    }

    public StopReadiness getReadiness() { return readiness; }
    public String getReadinessNote() { return readinessNote; }
    public Long getId() { return id; }
    public Long getProgrammeId() { return programmeId; }
    public Long getVenueId() { return venueId; }
    public String getTitle() { return title; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getNotes() { return notes; }
}
