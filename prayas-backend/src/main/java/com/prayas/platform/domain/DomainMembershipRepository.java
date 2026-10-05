package com.prayas.platform.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DomainMembershipRepository extends JpaRepository<DomainMembership, DomainMembership.Id> {

    @Query("""
           select count(m) > 0 from DomainMembership m
           where m.user.id = :userId
             and m.domain.code = :domainCode
             and m.domainRole = :role
           """)
    boolean existsByUserAndDomainCodeAndRole(@Param("userId") Long userId,
                                              @Param("domainCode") String domainCode,
                                              @Param("role") DomainRole role);

    @Query("select m from DomainMembership m join fetch m.domain where m.user.id = :userId")
    List<DomainMembership> findByUserId(@Param("userId") Long userId);

    @Query("select m from DomainMembership m join fetch m.domain join fetch m.user")
    List<DomainMembership> findAllWithDomainAndUser();

    // The two writes below are plain SQL on purpose. DomainMembership's key is
    // built from two @ManyToOne relationships, and nothing in the app had ever
    // *written* one through JPA (rows only ever arrived via SQL). Plain SQL
    // executes immediately and in order, so "replace everything for this
    // user" is a simple delete followed by inserts -- no persistence-context
    // ordering surprises.
    @Modifying(flushAutomatically = true)
    @Query(value = "delete from domain_membership where user_id = :userId", nativeQuery = true)
    void deleteAllForUser(@Param("userId") Long userId);

    @Modifying(flushAutomatically = true)
    @Query(value = "insert into domain_membership (user_id, domain_id, domain_role) "
            + "values (:userId, :domainId, :role)", nativeQuery = true)
    void addMembership(@Param("userId") Long userId,
                       @Param("domainId") Long domainId,
                       @Param("role") String role);
}
