package com.prayas.platform.tour;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface HeadcountRevisionRepository extends JpaRepository<HeadcountRevision, Long> {
    Optional<HeadcountRevision> findTopByProgrammeIdOrderByRevisionNoDesc(Long programmeId);
}
