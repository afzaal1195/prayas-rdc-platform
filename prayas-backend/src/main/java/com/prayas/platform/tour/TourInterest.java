package com.prayas.platform.tour;

import com.prayas.platform.venue.Venue;
import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Entity
@Table(name = "tour_interest")
@IdClass(TourInterest.Id.class)
public class TourInterest {

    @jakarta.persistence.Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "programme_id")
    private Programme programme;

    @jakarta.persistence.Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    protected TourInterest() {
        // JPA
    }

    public TourInterest(Programme programme, Venue venue) {
        this.programme = programme;
        this.venue = venue;
    }

    public static class Id implements Serializable {
        private Long programme;
        private Long venue;

        public Id() {
        }

        public Id(Long programme, Long venue) {
            this.programme = programme;
            this.venue = venue;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(programme, id.programme) && Objects.equals(venue, id.venue);
        }

        @Override
        public int hashCode() {
            return Objects.hash(programme, venue);
        }
    }
}
