package com.prayas.platform.itinerary;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;

public interface ItineraryStopRepository extends JpaRepository<ItineraryStop, Long> {

    List<ItineraryStop> findByProgrammeIdOrderByStartTimeAscIdAsc(Long programmeId);

    List<ItineraryStop> findByProgrammeIdInOrderByStartTimeAscIdAsc(Collection<Long> programmeIds);

    /**
     * Stops of OTHER approved tours that use the same venue on the same date at
     * an overlapping time. Two stops overlap when each starts before the other ends,
     * so one that ends exactly when the next begins is not a clash.
     */
    @Query("""
           select s from ItineraryStop s, Programme p
           where p.id = s.programmeId
             and p.status = com.prayas.platform.tour.ProgrammeStatus.APPROVED
             and p.visitDate = :date
             and s.venueId = :venueId
             and s.programmeId <> :excludeProgrammeId
             and s.startTime < :endTime
             and s.endTime > :startTime
           """)
    List<ItineraryStop> findVenueClashes(@Param("venueId") Long venueId,
                                         @Param("date") LocalDate date,
                                         @Param("startTime") LocalTime startTime,
                                         @Param("endTime") LocalTime endTime,
                                         @Param("excludeProgrammeId") Long excludeProgrammeId);
}
