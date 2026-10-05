package com.prayas.platform.tour;

import org.springframework.stereotype.Component;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDate;

/**
 * Takes a Postgres transaction-scoped advisory lock keyed by visit date, so
 * two leads approving *different* tours for the *same* date are serialized
 * and the daily-cap check in TourDecisionService can't race. The lock is
 * released automatically when the transaction commits or rolls back.
 */
@Component
public class AdvisoryLockDao {

    @PersistenceContext
    private EntityManager entityManager;

    public void lockDate(LocalDate date) {
        entityManager
                .createNativeQuery("select pg_advisory_xact_lock(:key)")
                .setParameter("key", date.toEpochDay())
                .getSingleResult();
    }
}
