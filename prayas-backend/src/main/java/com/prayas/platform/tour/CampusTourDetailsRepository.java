package com.prayas.platform.tour;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface CampusTourDetailsRepository extends JpaRepository<CampusTourDetails, Long> {
    Optional<CampusTourDetails> findByProgrammeId(Long programmeId);
    Optional<CampusTourDetails> findByTrackingTokenHash(String trackingTokenHash);
}
