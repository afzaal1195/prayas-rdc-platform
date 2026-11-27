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

    public Long getId() { return id; }
    public Long getProgrammeId() { return programmeId; }
    public Long getVenueId() { return venueId; }
    public String getTitle() { return title; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public String getNotes() { return notes; }
}
