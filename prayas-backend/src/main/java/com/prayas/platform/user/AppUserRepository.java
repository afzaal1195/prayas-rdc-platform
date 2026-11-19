package com.prayas.platform.user;

import com.prayas.platform.domain.DomainRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface AppUserRepository extends JpaRepository<AppUser, Long> {
    Optional<AppUser> findByEmailIgnoreCase(String email);

    /**
     * Active people who hold one of the given roles in any domain; with
     * includeFillers, also anyone cleared to fill volunteer slots.
     */
    @Query("""
           select u from AppUser u
           where u.active = true
             and (exists (select 1 from DomainMembership m
                          where m.user = u and m.domainRole in :roles)
                  or (:includeFillers = true and u.canFillVolunteerSlots = true))
           order by u.fullName
           """)
    List<AppUser> findEligible(@Param("roles") Collection<DomainRole> roles,
                               @Param("includeFillers") boolean includeFillers);
}
