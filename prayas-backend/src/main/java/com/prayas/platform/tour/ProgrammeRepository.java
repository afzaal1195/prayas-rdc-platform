package com.prayas.platform.tour;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDate;
import java.util.List;

public interface ProgrammeRepository extends JpaRepository<Programme, Long> {

    // @Version on Programme already guards against a stale concurrent update
    // to the *same* row. The advisory lock taken in TourDecisionService is
    // what serializes decisions across *different* programmes on one date.

    @Query("""
           select count(p) from Programme p
           where p.visitDate = :date and p.status = com.prayas.platform.tour.ProgrammeStatus.APPROVED
           """)
    long countApprovedOnDate(@Param("date") LocalDate date);

    List<Programme> findByVisitDateBetweenAndStatus(LocalDate from, LocalDate to, ProgrammeStatus status);

    List<Programme> findAllByOrderByIdDesc();

    List<Programme> findByStatusOrderByIdDesc(ProgrammeStatus status);
}