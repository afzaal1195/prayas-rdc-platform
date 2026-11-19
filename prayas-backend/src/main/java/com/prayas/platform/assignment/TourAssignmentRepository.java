package com.prayas.platform.assignment;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public interface TourAssignmentRepository extends JpaRepository<TourAssignment, Long> {

    List<TourAssignment> findByProgrammeIdAndStatusOrderByStartsAtAscIdAsc(Long programmeId, AssignmentStatus status);

    List<TourAssignment> findByUserIdAndStatusOrderByStartsAtAscIdAsc(Long userId, AssignmentStatus status);

    /** Takes everyone off a tour (used when its date moves, because the shifts were planned for the old date). */
    @Modifying
    @Query("""
           update TourAssignment a
           set a.status = com.prayas.platform.assignment.AssignmentStatus.RELEASED
           where a.programmeId = :programmeId
             and a.status = com.prayas.platform.assignment.AssignmentStatus.ACTIVE
           """)
    int releaseAllForProgramme(@Param("programmeId") Long programmeId);

    /** Is this person already on an active shift that overlaps [start, end)? Back-to-back shifts do not overlap. */
    @Query("""
           select count(a) > 0 from TourAssignment a
           where a.userId = :userId
             and a.status = com.prayas.platform.assignment.AssignmentStatus.ACTIVE
             and a.startsAt < :end and a.endsAt > :start
           """)
    boolean existsOverlap(@Param("userId") Long userId,
                          @Param("start") Instant start,
                          @Param("end") Instant end);

    /** Everyone who is already busy at any point in [start, end). */
    @Query("""
           select distinct a.userId from TourAssignment a
           where a.status = com.prayas.platform.assignment.AssignmentStatus.ACTIVE
             and a.startsAt < :end and a.endsAt > :start
           """)
    Set<Long> findBusyUserIds(@Param("start") Instant start, @Param("end") Instant end);
}
