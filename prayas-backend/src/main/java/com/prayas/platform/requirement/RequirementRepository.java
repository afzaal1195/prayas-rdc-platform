package com.prayas.platform.requirement;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RequirementRepository extends JpaRepository<Requirement, Long> {

    @Query("select r from Requirement r where r.programme.id = :programmeId and r.kind = :kind")
    Optional<Requirement> findByProgrammeIdAndKind(@Param("programmeId") Long programmeId,
                                                     @Param("kind") RequirementKind kind);

    @Query("select r from Requirement r where r.programme.id = :programmeId")
    List<Requirement> findByProgrammeId(@Param("programmeId") Long programmeId);

    List<Requirement> findByDomainId(Long domainId);

    List<Requirement> findByDomainIdAndStatus(Long domainId, RequirementStatus status);
}