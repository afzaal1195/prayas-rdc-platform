package com.prayas.platform.tour;

import com.prayas.platform.school.School;
import com.prayas.platform.school.SchoolRepository;
import com.prayas.platform.venue.Venue;
import com.prayas.platform.venue.VenueRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
public class PublicTourRequestService {

    private final ProgrammeRepository programmeRepository;
    private final CampusTourDetailsRepository tourDetailsRepository;
    private final HeadcountRevisionRepository headcountRepository;
    private final TourInterestRepository tourInterestRepository;
    private final TourTeacherRepository tourTeacherRepository;
    private final SchoolRepository schoolRepository;
    private final VenueRepository venueRepository;
    private final TrackingTokenGenerator tokenGenerator;
    private final CaptchaVerifier captchaVerifier;
    private final TourProperties props;
    private final TourDecisionService decisionService;

    public PublicTourRequestService(ProgrammeRepository programmeRepository,
                                     CampusTourDetailsRepository tourDetailsRepository,
                                     HeadcountRevisionRepository headcountRepository,
                                     TourInterestRepository tourInterestRepository,
                                     TourTeacherRepository tourTeacherRepository,
                                     SchoolRepository schoolRepository,
                                     VenueRepository venueRepository,
                                     TrackingTokenGenerator tokenGenerator,
                                     CaptchaVerifier captchaVerifier,
                                     TourProperties props,
                                     TourDecisionService decisionService) {
        this.programmeRepository = programmeRepository;
        this.tourDetailsRepository = tourDetailsRepository;
        this.headcountRepository = headcountRepository;
        this.tourInterestRepository = tourInterestRepository;
        this.tourTeacherRepository = tourTeacherRepository;
        this.schoolRepository = schoolRepository;
        this.venueRepository = venueRepository;
        this.tokenGenerator = tokenGenerator;
        this.captchaVerifier = captchaVerifier;
        this.props = props;
        this.decisionService = decisionService;
    }

    @Transactional
    public TourRequestCreated submit(TourRequestCreate request, String remoteIp) {
        // Honeypot: a real school never fills a field named "website" that's
        // hidden by CSS. Silently pretend success so bots don't learn to skip it.
        if (request.website() != null && !request.website().isBlank()) {
            return fakeSuccessForBots();
        }
        if (!captchaVerifier.verify(request.captchaToken(), remoteIp)) {
            throw new IllegalArgumentException("Captcha verification failed");
        }

        validateVisitDate(request.visitDate());
        validateTimes(request.arrivalTime(), request.departureTime());
        Breakdown breakdown = resolveBreakdown(request.institutionType(), request.grades(), request.courses());
        Integer mealCount = validateLunch(request.lunch());

        School school = new School(request.school().name());
        school.setAddress(request.school().address());
        school.setVillageOrTown(titleCase(request.school().villageOrTown()));
        school.setDistrict(titleCase(request.school().district()));
        school.setState(titleCase(request.school().state()));
        schoolRepository.save(school);

        Programme programme = new Programme(request.visitDate());
        programme.setArrivalTime(request.arrivalTime());
        programme.setDepartureTime(request.departureTime());
        programmeRepository.save(programme);

        TrackingTokenGenerator.TokenPair token = tokenGenerator.generate();
        CampusTourDetails details = new CampusTourDetails(
                programme, school, request.contact().name(), request.contact().phone(), token.hash());
        details.setInstitutionType(request.institutionType());
        details.setContactEmail(request.contact().email());
        details.setGradeRange(breakdown.displayText());
        details.setVehicleNumber(request.vehicleNumber());
        details.setLunchRequired(request.lunch().required());
        details.setInterestNotes(request.interestNotes());
        tourDetailsRepository.save(details);

        for (TourRequestCreate.TeacherInfo teacher : request.teachers()) {
            tourTeacherRepository.save(new TourTeacher(programme, teacher.name(), teacher.phone()));
        }

        List<Venue> venues = venueRepository.findAllById(request.venueIds());
        for (Venue venue : venues) {
            tourInterestRepository.save(new TourInterest(programme, venue));
        }

        // Public form collects one combined headcount for catering rather than
        // a veg/non-veg split -- see TourRequestCreate.LunchInfo. Stored here
        // as vegMeals with nonVegMeals left null; H&L can clarify the actual
        // breakdown directly with the school when confirming lunch, same way
        // they already make contact for venue approvals.
        HeadcountRevision revision = new HeadcountRevision(
                programme, 1, breakdown.totalStudents(), request.teachers().size(), null);
        revision.setVegMeals(mealCount);
        headcountRepository.save(revision);

        programme.transitionTo(ProgrammeStatus.UNDER_REVIEW);

        return new TourRequestCreated(programme.getId(), token.rawToken(), programme.getStatus());
    }

    private void validateVisitDate(LocalDate visitDate) {
        LocalDate earliestAllowed = LocalDate.now().plusDays(props.getMinAdvanceDays());
        if (visitDate.isBefore(earliestAllowed)) {
            throw new IllegalArgumentException(
                    "Visit date must be at least %d days from today".formatted(props.getMinAdvanceDays()));
        }
    }

    private void validateTimes(LocalTime arrival, LocalTime departure) {
        LocalTime earliest = LocalTime.parse(props.getEarliestArrival());
        LocalTime latest = LocalTime.parse(props.getLatestDeparture());

        if (arrival != null && (arrival.isBefore(earliest) || arrival.isAfter(latest))) {
            throw new IllegalArgumentException(
                    "Arrival time must be between %s and %s".formatted(earliest, latest));
        }
        if (departure != null && (departure.isBefore(earliest) || departure.isAfter(latest))) {
            throw new IllegalArgumentException(
                    "Departure time must be between %s and %s".formatted(earliest, latest));
        }
        if (arrival != null && departure != null) {
            long gapMinutes = Duration.between(arrival, departure).toMinutes();
            if (gapMinutes < props.getMinVisitDurationHours() * 60L) {
                throw new IllegalArgumentException(
                        "There must be at least %d hours between arrival and departure"
                                .formatted(props.getMinVisitDurationHours()));
            }
        }
    }

    /** Display text for the grade_range column, plus the derived total student count. */
    private record Breakdown(String displayText, int totalStudents) {
    }

    /**
     * Replaces what used to be a single free-typed grade range / course
     * field: the public form now collects a repeatable list of
     * grade-and-count (school) or course-and-count (college) rows, the same
     * pattern as the teacher list. Total student count is derived by
     * summing these rows rather than being typed separately, so it can
     * never drift out of sync with the breakdown. Shared by submit() and
     * reviseHeadcount() so a revision's new breakdown is formatted and
     * validated exactly the same way the original submission was.
     */
    private Breakdown resolveBreakdown(InstitutionType institutionType,
                                        List<TourRequestCreate.GradeCount> grades,
                                        List<TourRequestCreate.CourseCount> courses) {
        if (institutionType == InstitutionType.COLLEGE) {
            if (courses == null || courses.isEmpty()) {
                throw new IllegalArgumentException("Add at least one course/branch with a student count");
            }
            int total = 0;
            List<String> parts = new java.util.ArrayList<>();
            for (TourRequestCreate.CourseCount c : courses) {
                total += c.count();
                String year = c.yearOfStudy() == null ? "" : c.yearOfStudy().trim();
                String label = year.isEmpty() ? c.courseOrBranch().trim() : "%s (%s)".formatted(c.courseOrBranch().trim(), year);
                parts.add("%s: %d".formatted(label, c.count()));
            }
            validateTotal(total);
            return new Breakdown(String.join(", ", parts), total);
        }

        if (grades == null || grades.isEmpty()) {
            throw new IllegalArgumentException("Add at least one grade with a student count");
        }
        int total = 0;
        List<String> parts = new java.util.ArrayList<>();
        for (TourRequestCreate.GradeCount g : grades) {
            total += g.count();
            parts.add("Grade %s: %d".formatted(g.grade(), g.count()));
        }
        validateTotal(total);
        return new Breakdown(String.join(", ", parts), total);
    }

    /**
     * Reverses resolveBreakdown's text formatting, so the tracking page can
     * pre-fill the current grade/course rows instead of starting empty.
     * Safe because we fully control the format being parsed here (it's
     * never free-typed by a user) -- see resolveBreakdown for the exact
     * strings this must stay in sync with.
     */
    private record ParsedBreakdown(List<TourRequestCreate.GradeCount> grades,
                                    List<TourRequestCreate.CourseCount> courses) {
    }

    private ParsedBreakdown parseBreakdown(InstitutionType institutionType, String gradeRangeText) {
        if (gradeRangeText == null || gradeRangeText.isBlank()) {
            return new ParsedBreakdown(List.of(), List.of());
        }
        String[] segments = gradeRangeText.split(",\\s*");

        if (institutionType == InstitutionType.COLLEGE) {
            List<TourRequestCreate.CourseCount> courses = new java.util.ArrayList<>();
            java.util.regex.Pattern p = java.util.regex.Pattern.compile("^(.+?)(?: \\((.+)\\))?: (\\d+)$");
            for (String seg : segments) {
                java.util.regex.Matcher m = p.matcher(seg.trim());
                if (m.matches()) {
                    courses.add(new TourRequestCreate.CourseCount(m.group(1), m.group(2), Integer.parseInt(m.group(3))));
                }
            }
            return new ParsedBreakdown(List.of(), courses);
        }

        List<TourRequestCreate.GradeCount> grades = new java.util.ArrayList<>();
        java.util.regex.Pattern p = java.util.regex.Pattern.compile("^Grade (\\d+): (\\d+)$");
        for (String seg : segments) {
            java.util.regex.Matcher m = p.matcher(seg.trim());
            if (m.matches()) {
                grades.add(new TourRequestCreate.GradeCount(m.group(1), Integer.parseInt(m.group(2))));
            }
        }
        return new ParsedBreakdown(grades, List.of());
    }

    private void validateTotal(int total) {
        if (total < 1 || total > 500) {
            throw new IllegalArgumentException("Total student count across all rows must be between 1 and 500");
        }
    }

    /**
     * Normalizes location fields (village/town, district, state) to Title
     * Case before persisting, so "hyderabad", "HYDERABAD", and "Hyderabad"
     * all end up as one consistent value -- makes WHERE/GROUP BY queries on
     * these columns reliable without extra LOWER()/normalization at query
     * time. Deliberately never applied to people's names or school names,
     * which should be stored exactly as given.
     */
    private String titleCase(String input) {
        if (input == null || input.isBlank()) {
            return input;
        }
        String[] words = input.trim().toLowerCase().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
            }
            result.append(' ');
        }
        return result.toString().trim();
    }

    private Integer validateLunch(TourRequestCreate.LunchInfo lunch) {
        if (lunch.required() && (lunch.count() == null || lunch.count() < 1)) {
            throw new IllegalArgumentException("Meal count is required when lunch is needed");
        }
        return lunch.required() ? lunch.count() : null;
    }

    @Transactional(readOnly = true)
    public TourStatusView status(String rawToken) {
        CampusTourDetails details = tourDetailsRepository
                .findByTrackingTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> new NoSuchElementException("No tour request for this link"));
        Optional<HeadcountRevision> latest = headcountRepository
                .findTopByProgrammeIdOrderByRevisionNoDesc(details.getProgramme().getId());
        int currentCount = latest.map(HeadcountRevision::getStudentCount).orElse(0);
        Integer mealCount = latest.map(HeadcountRevision::getVegMeals).orElse(null);
        ParsedBreakdown breakdown = parseBreakdown(details.getInstitutionType(), details.getGradeRange());
        return TourStatusView.from(details.getProgramme(), details, currentCount, mealCount,
                breakdown.grades(), breakdown.courses());
    }

    /**
     * Re-entering the breakdown, not editing a running total: the school
     * sends their updated grade/course rows, the same shape as a fresh
     * submission, and this derives the new total and re-formats
     * grade_range the same way -- so the next status() load reflects what
     * was just submitted instead of the original, now-stale breakdown.
     */
    @Transactional
    public void reviseHeadcount(String rawToken, List<TourRequestCreate.GradeCount> grades,
                                 List<TourRequestCreate.CourseCount> courses, int newTeacherCount,
                                 boolean lunchRequired, Integer mealCount) {
        CampusTourDetails details = tourDetailsRepository
                .findByTrackingTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> new NoSuchElementException("No tour request for this link"));
        Programme programme = details.getProgramme();

        LocalDate freezeDate = programme.getVisitDate().minusDays(props.getHeadcountFreezeDays());
        if (!LocalDate.now().isBefore(freezeDate)) {
            throw new AccessDeniedException(
                    "Headcount is frozen from %s; contact Prayas directly to change it".formatted(freezeDate));
        }
        if (lunchRequired && (mealCount == null || mealCount < 1)) {
            throw new IllegalArgumentException("Meal count is required when lunch is needed");
        }

        Breakdown breakdown = resolveBreakdown(details.getInstitutionType(), grades, courses);

        int nextRevisionNo = headcountRepository
                .findTopByProgrammeIdOrderByRevisionNoDesc(programme.getId())
                .map(r -> r.getRevisionNo() + 1)
                .orElse(1);
        HeadcountRevision revision = new HeadcountRevision(
                programme, nextRevisionNo, breakdown.totalStudents(), newTeacherCount, null);
        revision.setVegMeals(lunchRequired ? mealCount : null);
        headcountRepository.save(revision);
        details.setLunchRequired(lunchRequired);
        details.setGradeRange(breakdown.displayText());

        if (programme.getStatus() == ProgrammeStatus.APPROVED) {
            decisionService.flagRequirementsForHeadcountChange(programme.getId());
        }
    }

    /**
     * Lets the school cancel their own request at any point before it's in
     * a final state. The reason is stored in the same field staff use for
     * a rejection note -- either way it's "why this status changed", shown
     * back to the school on the tracking page.
     */
    @Transactional
    public void cancel(String rawToken, String reason) {
        CampusTourDetails details = tourDetailsRepository
                .findByTrackingTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> new NoSuchElementException("No tour request for this link"));
        Programme programme = details.getProgramme();
        // transitionTo already throws IllegalStateException (-> 409) for a
        // final-state programme (REJECTED/CANCELLED/COMPLETED), which is
        // exactly the right behavior here -- no extra check needed.
        programme.transitionTo(ProgrammeStatus.CANCELLED);
        if (reason != null && !reason.isBlank()) {
            programme.setDecisionNote(reason);
        }
    }

    /**
     * The school asking to push their own request to a later date (and
     * optionally new arrival/departure times), only while it's still
     * awaiting a decision -- distinct from staff proposing a date
     * (RESCHEDULE_PROPOSED), which goes through the decision endpoint
     * instead. The new date must be later than the one currently on file
     * (this is about pushing a request back, not pulling it in) and still
     * satisfies the same minimum-advance-notice and visit-hours rules as a
     * brand new submission.
     */
    @Transactional
    public void requestDateChange(String rawToken, LocalDate newVisitDate,
                                   LocalTime newArrivalTime, LocalTime newDepartureTime) {
        CampusTourDetails details = tourDetailsRepository
                .findByTrackingTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(() -> new NoSuchElementException("No tour request for this link"));
        Programme programme = details.getProgramme();

        if (programme.getStatus() != ProgrammeStatus.SUBMITTED
                && programme.getStatus() != ProgrammeStatus.UNDER_REVIEW) {
            throw new AccessDeniedException(
                    "You can only request a different date before a decision has been made");
        }
        if (!newVisitDate.isAfter(programme.getVisitDate())) {
            throw new IllegalArgumentException(
                    "The new date must be later than the currently requested date (%s)"
                            .formatted(programme.getVisitDate()));
        }
        validateVisitDate(newVisitDate);

        LocalTime arrival = newArrivalTime != null ? newArrivalTime : programme.getArrivalTime();
        LocalTime departure = newDepartureTime != null ? newDepartureTime : programme.getDepartureTime();
        validateTimes(arrival, departure);

        programme.changeVisitDate(newVisitDate);
        programme.setArrivalTime(arrival);
        programme.setDepartureTime(departure);
    }

    private TourRequestCreated fakeSuccessForBots() {
        return new TourRequestCreated(0L, tokenGenerator.generate().rawToken(), ProgrammeStatus.UNDER_REVIEW);
    }
}