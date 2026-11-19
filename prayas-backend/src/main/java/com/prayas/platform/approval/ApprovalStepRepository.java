package com.prayas.platform.approval;

import com.prayas.platform.tour.ProgrammeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ApprovalStepRepository extends JpaRepository<ApprovalStep, Long> {

    List<ApprovalStep> findByProgrammeIdOrderBySequenceAscIdAsc(Long programmeId);

    /** Every step of every approved tour on or after {@code today}; the service keeps the tours that still have something open. */
    @Query("""
           select s from ApprovalStep s
           where s.programmeId in (select p.id from Programme p
                                   where p.status = :status and p.visitDate >= :today)
           order by s.programmeId, s.sequence, s.id
           """)
    List<ApprovalStep> findForUpcomingTours(@Param("status") ProgrammeStatus status,
                                            @Param("today") LocalDate today);
}
