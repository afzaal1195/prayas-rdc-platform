package com.prayas.platform.tour;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CampusTourDetailsRepository extends JpaRepository<CampusTourDetails, Long> {
    Optional<CampusTourDetails> findByProgrammeId(Long programmeId);
    Optional<CampusTourDetails> findByTrackingTokenHash(String trackingTokenHash);

    /**
     * Requests that should stop the same school sending another: anything in one
     * of the pending statuses, plus approved tours whose visit date has not passed.
     * Nothing in the app marks a tour COMPLETED yet, so an approved tour is treated
     * as finished once its visit date is behind us. Schools are loaded in the same
     * query. Used to spot repeat requests.
     */
    @Query("""
            select d from CampusTourDetails d
            join fetch d.school
            join d.programme p
            where p.status in :pendingStatuses
               or (p.status = com.prayas.platform.tour.ProgrammeStatus.APPROVED
                   and p.visitDate >= :today)
            """)
    List<CampusTourDetails> findBlockingRequests(
            @Param("pendingStatuses") Collection<ProgrammeStatus> pendingStatuses,
            @Param("today") LocalDate today);
}