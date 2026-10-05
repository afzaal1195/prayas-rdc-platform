package com.prayas.platform.tour;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TourInterestRepository extends JpaRepository<TourInterest, TourInterest.Id> {

    @Query("""
           select count(ti) > 0 from TourInterest ti
           where ti.programme.id = :programmeId and ti.venue.requiresApproval = true
           """)
    boolean anyRequiresApproval(@Param("programmeId") Long programmeId);

    /** How many tour requests picked this venue -- if any did, it can't be deleted. */
    @Query("select count(ti) from TourInterest ti where ti.venue.id = :venueId")
    long countUsesOfVenue(@Param("venueId") Long venueId);
}
