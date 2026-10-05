package com.prayas.platform.venue;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface VenueRepository extends JpaRepository<Venue, Long> {
    List<Venue> findByPublicVisibleTrueAndActiveTrue();

    List<Venue> findAllByOrderByNameAsc();

    /** Venues that name this contact as their approver -- if any do, the contact can't be deleted. */
    @Query("select v from Venue v where v.authorityContact.id = :authorityId order by v.name")
    List<Venue> findUsingAuthority(@Param("authorityId") Long authorityId);
}
