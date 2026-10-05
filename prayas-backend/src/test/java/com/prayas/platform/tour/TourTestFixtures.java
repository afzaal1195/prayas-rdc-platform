package com.prayas.platform.tour;

import com.prayas.platform.school.School;
import com.prayas.platform.school.SchoolRepository;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import com.prayas.platform.user.GlobalRole;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.time.LocalDate;

/**
 * Test-only helper that seeds the minimum data TourDecisionServiceConcurrencyTest
 * needs: N SUBMITTED programmes -- each with its own school and campus tour
 * details, since TourDecisionService.approve() looks up CampusTourDetails and
 * throws if it's missing -- all sharing one visit date, plus a single LEAD
 * user allowed to decide them. Domains (HOSPITALITY_LOGISTICS, VCU, etc.) are
 * already seeded by the V1 Flyway migration, so nothing to do there.
 */
@Component
public class TourTestFixtures {

    private final SchoolRepository schoolRepository;
    private final ProgrammeRepository programmeRepository;
    private final CampusTourDetailsRepository tourDetailsRepository;
    private final AppUserRepository appUserRepository;

    public TourTestFixtures(SchoolRepository schoolRepository,
                             ProgrammeRepository programmeRepository,
                             CampusTourDetailsRepository tourDetailsRepository,
                             AppUserRepository appUserRepository) {
        this.schoolRepository = schoolRepository;
        this.programmeRepository = programmeRepository;
        this.tourDetailsRepository = tourDetailsRepository;
        this.appUserRepository = appUserRepository;
    }

    /** Creates {@code count} independent SUBMITTED programmes for the same date, ready to be raced. */
    @Transactional
    public List<Long> createSubmittedProgrammes(LocalDate visitDate, int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            School school = schoolRepository.save(new School("Race Test School " + i));
            Programme programme = programmeRepository.save(new Programme(visitDate));
            programme.transitionTo(ProgrammeStatus.UNDER_REVIEW); // matches PublicTourRequestService.submit()

            // 64 hex chars, mimicking a real SHA-256 tracking-token hash; only
            // needs to be unique per row, which two concatenated UUIDs guarantee.
            String fakeHash = (UUID.randomUUID().toString() + UUID.randomUUID())
                    .replace("-", "");

            CampusTourDetails details = new CampusTourDetails(
                    programme, school, "Contact " + i, "9000000000", fakeHash);
            details.setContactEmail("race-test-" + i + "@example.com");
            tourDetailsRepository.save(details);

            ids.add(programme.getId());
        }
        return ids;
    }

    /** A single reusable LEAD user, created once and reused across test runs against the same DB. */
    public AppUser anyLeadUser() {
        return appUserRepository.findByEmailIgnoreCase("race-test-lead@iith.ac.in")
                .orElseGet(() -> {
                    AppUser user = new AppUser("race-test-lead@iith.ac.in", "Race Test Lead");
                    user.setGlobalRole(GlobalRole.LEAD);
                    return appUserRepository.save(user);
                });
    }
}