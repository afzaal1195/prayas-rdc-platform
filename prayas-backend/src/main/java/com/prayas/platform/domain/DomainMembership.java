package com.prayas.platform.domain;

import com.prayas.platform.user.AppUser;
import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;

@Entity
@Table(name = "domain_membership")
@IdClass(DomainMembership.Id.class)
public class DomainMembership {

    @jakarta.persistence.Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private AppUser user;

    @jakarta.persistence.Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "domain_id")
    private Domain domain;

    @Enumerated(EnumType.STRING)
    @Column(name = "domain_role", nullable = false)
    private DomainRole domainRole;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private Instant joinedAt = Instant.now();

    protected DomainMembership() {
        // JPA
    }

    public DomainMembership(AppUser user, Domain domain, DomainRole domainRole) {
        this.user = user;
        this.domain = domain;
        this.domainRole = domainRole;
    }

    public AppUser getUser() {
        return user;
    }

    public Domain getDomain() {
        return domain;
    }

    public DomainRole getDomainRole() {
        return domainRole;
    }

    public void setDomainRole(DomainRole domainRole) {
        this.domainRole = domainRole;
    }

    /** Composite key mirroring (user_id, domain_id). */
    public static class Id implements Serializable {
        private Long user;
        private Long domain;

        public Id() {
        }

        public Id(Long user, Long domain) {
            this.user = user;
            this.domain = domain;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Id id)) return false;
            return Objects.equals(user, id.user) && Objects.equals(domain, id.domain);
        }

        @Override
        public int hashCode() {
            return Objects.hash(user, domain);
        }
    }
}
