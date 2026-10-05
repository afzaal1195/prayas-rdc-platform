package com.prayas.platform.tour;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Proves the daily cap (set to 3 for Prayas) actually holds when several
 * leads approve different tours for the same date at the same instant --
 * the scenario the advisory lock in TourDecisionService exists for.
 *
 * Runs against a real local Postgres (prayas_test database, same server as
 * dev) rather than Testcontainers: pg_advisory_xact_lock has no H2
 * equivalent worth trusting, and Testcontainers currently can't start on
 * this machine's Docker Desktop (Windows/npipe + Docker Engine v29 broke
 * an internal image-inspect call Testcontainers' Ryuk watchdog relies on --
 * revisit with a native Ubuntu/WSL Docker install later).
 *
 * Create the database once with: CREATE DATABASE prayas_test OWNER prayas;
 */
@SpringBootTest
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/prayas_test",
        "spring.datasource.username=prayas",
        "spring.datasource.password=changeme"
})
class TourDecisionServiceConcurrencyTest {

    @Autowired
    private TourDecisionService decisionService;

    @Autowired
    private TourTestFixtures fixtures; // seeds a school, and N SUBMITTED programmes for one date

    @Test
    void onlyMaxPerDayApprovalsSucceedWhenRacedConcurrently() throws InterruptedException {
        // A fixed offset (e.g. plusDays(30)) would resolve to the same date on
        // every run today, and since this runs against a real persistent
        // Postgres (not a fresh Testcontainers instance each time), leftover
        // approved programmes from earlier runs would eat into today's cap.
        // A random far-future date keeps every run isolated.
        LocalDate sameDate = LocalDate.now().plusDays(1000 + ThreadLocalRandom.current().nextInt(100000));
        int candidateCount = 6; // more requests than the cap, to force contention
        List<Long> programmeIds = fixtures.createSubmittedProgrammes(sameDate, candidateCount);

        ExecutorService pool = Executors.newFixedThreadPool(candidateCount);
        CountDownLatch startGate = new CountDownLatch(1);
        AtomicInteger approved = new AtomicInteger();
        AtomicInteger rejectedByCapacity = new AtomicInteger();

        List<Future<?>> futures = new ArrayList<>();
        for (Long id : programmeIds) {
            futures.add(pool.submit(() -> {
                try {
                    startGate.await();
                    decisionService.decide(id,
                            new DecisionRequest(DecisionType.APPROVE, "race test", null),
                            fixtures.anyLeadUser());
                    approved.incrementAndGet();
                } catch (DailyCapExceededException e) {
                    rejectedByCapacity.incrementAndGet();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }));
        }

        startGate.countDown(); // release all threads at once
        for (Future<?> f : futures) {
            try {
                f.get(10, TimeUnit.SECONDS);
            } catch (ExecutionException | TimeoutException e) {
                e.printStackTrace(); // surface the real cause instead of hiding it
            }
        }
        pool.shutdown();

        assertThat(approved.get()).isEqualTo(3); // TourProperties.maxPerDay for Prayas
        assertThat(rejectedByCapacity.get()).isEqualTo(candidateCount - 3);
    }
}