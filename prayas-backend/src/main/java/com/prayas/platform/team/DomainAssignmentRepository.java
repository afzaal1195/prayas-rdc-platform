package com.prayas.platform.team;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface DomainAssignmentRepository extends JpaRepository<DomainAssignment, Long> {

    List<DomainAssignment> findByProgrammeIdIn(Collection<Long> programmeIds);

    List<DomainAssignment> findByUserId(Long userId);

    boolean existsByProgrammeIdAndDomainIdAndUserId(Long programmeId, Long domainId, Long userId);

    boolean existsByProgrammeIdAndDomainIdAndUserIdAndRole(Long programmeId, Long domainId, Long userId, TeamRole role);
}
