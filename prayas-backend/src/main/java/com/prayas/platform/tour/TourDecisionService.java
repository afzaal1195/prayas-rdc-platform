package com.prayas.platform.tour;

import com.prayas.platform.audit.AuditLog;
import com.prayas.platform.audit.AuditLogRepository;
import com.prayas.platform.domain.Domain;
import com.prayas.platform.domain.DomainRepository;
import com.prayas.platform.requirement.Requirement;
import com.prayas.platform.requirement.RequirementKind;
import com.prayas.platform.requirement.RequirementRepository;
import com.prayas.platform.user.AppUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

/**
 * Owns the one transaction that matters in phase 1: deciding a tour request.
 * See phase1-design.md section 5 for the full walkthrough of why each step
 * is here (advisory lock, optimistic version, requirement spawning order).
 */
@Service
public class TourDecisionService {

    private static final String HOSPITALITY_LOGISTICS = "HOSPITALITY_LOGISTICS";
    private static final String VCU = "VCU";

    private final ProgrammeRepository programmeRepository;
    private final CampusTourDetailsRepository tourDetailsRepository;
    private final HeadcountRevisionRepository headcountRepository;
    private final TourInterestRepository tourInterestRepository;
    private final RequirementRepository requirementRepository;
    private final DomainRepository domainRepository;
    private final AuditLogRepository auditLogRepository;
    private final AdvisoryLockDao advisoryLockDao;
    private final ApplicationEventPublisher events;
    private final TourProperties props;
    private final TrackingTokenGenerator tokenGenerator;
    private final TourTeacherRepository teacherRepository;

    public TourDecisionService(ProgrammeRepository programmeRepository,
                                CampusTourDetailsRepository tourDetailsRepository,
                                HeadcountRevisionRepository headcountRepository,
                                TourInterestRepository tourInterestRepository,
                                RequirementRepository requirementRepository,
                                DomainRepository domainRepository,
                                AuditLogRepository auditLogRepository,
                                AdvisoryLockDao advisoryLockDao,
                                ApplicationEventPublisher events,
                                TourProperties props,
                                TrackingTokenGenerator tokenGenerator,
                                TourTeacherRepository teacherRepository) {
        this.programmeRepository = programmeRepository;
        this.tourDetailsRepository = tourDetailsRepository;
        this.headcountRepository = headcountRepository;
        this.tourInterestRepository = tourInterestRepository;
        this.requirementRepository = requirementRepository;
        this.domainRepository = domainRepository;
        this.auditLogRepository = auditLogRepository;
        this.advisoryLockDao = advisoryLockDao;
        this.events = events;
        this.props = props;
        this.teacherRepository = teacherRepository;
        this.tokenGenerator = tokenGenerator;
    }

    /** Most-recent-first list for the staff dashboard's request queue. */
    @Transactional(readOnly = true)
    public List<ProgrammeSummaryView> listAll() {
        return programmeRepository.findAllByOrderByIdDesc().stream()
                .map(this::toSummary)
                .toList();
    }

    private ProgrammeSummaryView toSummary(Programme programme) {
        CampusTourDetails details = tourDetailsRepository.findByProgrammeId(programme.getId()).orElse(null);
        int studentCount = headcountRepository.findTopByProgrammeIdOrderByRevisionNoDesc(programme.getId())
                .map(HeadcountRevision::getStudentCount)
                .orElse(0);
        return new ProgrammeSummaryView(
                programme.getId(),
                details != null ? details.getSchool().getName() : "(no details)",
                details != null ? details.getSchool().getVillageOrTown() : null,
                details != null ? details.getSchool().getDistrict() : null,
                details != null ? details.getInstitutionType() : InstitutionType.SCHOOL,
                programme.getVisitDate(),
                programme.getArrivalTime(),
                programme.getDepartureTime(),
                programme.getStatus(),
                studentCount,
                details != null ? details.getContactName() : null,
                details != null ? details.getContactPhone() : null
        );
    }

    /** Everything the summary row doesn't show -- fetched lazily when a staff dashboard row is expanded. */
    @Transactional(readOnly = true)
    public ProgrammeDetailView detail(Long programmeId) {
        CampusTourDetails details = tourDetailsRepository.findByProgrammeId(programmeId)
                .orElseThrow(() -> new NoSuchElementException("No details found for programme " + programmeId));
        Programme programme = details.getProgramme();

        List<ProgrammeDetailView.TeacherInfo> teachers = teacherRepository.findByProgrammeId(programmeId).stream()
                .map(t -> new ProgrammeDetailView.TeacherInfo(t.getFullName(), t.getPhone()))
                .toList();

        Optional<HeadcountRevision> latest = headcountRepository.findTopByProgrammeIdOrderByRevisionNoDesc(programmeId);
        int teacherCount = latest.map(HeadcountRevision::getTeacherCount).orElse(0);
        Integer mealCount = latest.map(HeadcountRevision::getVegMeals).orElse(null);

        return new ProgrammeDetailView(
                details.getSchool().getAddress(),
                details.getSchool().getState(),
                details.getContactEmail(),
                details.getGradeRange(),
                teacherCount,
                teachers,
                details.isLunchRequired(),
                mealCount,
                details.getVehicleNumber(),
                details.getInterestNotes(),
                programme.getDecisionNote()
        );
    }

    /**
     * Staff issuing a fresh tracking link when a school has lost theirs.
     * Since only the link's hash is ever stored (see TrackingTokenGenerator),
     * the original lost link can never be recovered -- this replaces it
     * with a new one instead, which invalidates the old one if it still
     * existed. Any logged-in, recognized staff member may do this (not
     * gated behind canDecide -- it's a low-risk convenience action, not a
     * decision).
     */
    @Transactional
    public String regenerateTrackingLink(Long programmeId) {
        CampusTourDetails details = tourDetailsRepository.findByProgrammeId(programmeId)
                .orElseThrow(() -> new NoSuchElementException("No details found for programme " + programmeId));
        TrackingTokenGenerator.TokenPair token = tokenGenerator.generate();
        details.regenerateTrackingTokenHash(token.hash());
        return token.rawToken();
    }

    @Transactional
    public Programme decide(Long programmeId, DecisionRequest request, AppUser actor) {
        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new NoSuchElementException("Programme " + programmeId + " not found"));

        return switch (request.decision()) {
            case APPROVE -> approve(programme, request, actor);
            case REJECT -> reject(programme, request, actor);
            case PROPOSE_DATE -> proposeDate(programme, request, actor);
        };
    }

    /**
     * Takes a rejection back so the request can be decided again. Only a
     * REJECTED request can be reopened. The old rejection reason is cleared
     * (so the school's tracking page doesn't keep showing it against a request
     * that is under review again) but kept in the audit log, along with who
     * reopened it and why.
     */
    @Transactional
    public Programme reopen(Long programmeId, String reason, AppUser actor) {
        Programme programme = programmeRepository.findById(programmeId)
                .orElseThrow(() -> new NoSuchElementException("Programme " + programmeId + " not found"));
        if (programme.getStatus() != ProgrammeStatus.REJECTED) {
            throw new IllegalStateException("Only a rejected request can be reopened.");
        }
        String previousRejectionNote = Optional.ofNullable(programme.getDecisionNote()).orElse("");

        programme.transitionTo(ProgrammeStatus.UNDER_REVIEW);
        programme.setDecidedBy(null);
        programme.setDecidedAt(null);
        programme.setDecisionNote(null);

        auditLogRepository.save(AuditLog.byUser(actor.getId(), "TOUR_REOPENED", "Programme",
                programme.getId(), Map.of("reason", reason, "previousRejectionNote", previousRejectionNote)));
        return programme;
    }

    private Programme approve(Programme programme, DecisionRequest request, AppUser actor) {
        // Step 1: serialize with any other decision on the same date before
        // we count how many tours are already approved for it.
        advisoryLockDao.lockDate(programme.getVisitDate());

        // Step 2: daily cap, checked before any mutation so this programme's
        // own (not-yet-made) status change can never get auto-flushed into
        // the count and inflate it by one.
        long alreadyApproved = programmeRepository.countApprovedOnDate(programme.getVisitDate());
        if (alreadyApproved >= props.getMaxPerDay()) {
            throw new DailyCapExceededException(
                    "Daily cap of %d tours reached for %s".formatted(props.getMaxPerDay(), programme.getVisitDate()));
        }

        // Step 3: status transition (throws if not allowed; @Version guards
        // against a stale concurrent edit to this specific programme).
        programme.transitionTo(ProgrammeStatus.APPROVED);

        // Step 4: record the decision.
        programme.setDecidedBy(actor);
        programme.setDecidedAt(Instant.now());
        programme.setDecisionNote(request.note());

        // Step 5: spawn domain requirements (idempotent: unique on programme+kind).
        LocalDate dueDate = programme.getVisitDate().minusDays(props.getRequirementLeadDays());
        spawnRequirementsOnApproval(programme, dueDate);

        // Step 6: audit trail.
        auditLogRepository.save(AuditLog.byUser(actor.getId(), "TOUR_APPROVED", "Programme",
                programme.getId(), Map.of("note", Optional.ofNullable(request.note()).orElse(""))));

        // Step 7: notifications happen after commit, so a mail failure can
        // never roll back an approval that already succeeded.
        events.publishEvent(new TourApprovedEvent(programme.getId()));

        return programme;
    }

    private void spawnRequirementsOnApproval(Programme programme, LocalDate dueDate) {
        CampusTourDetails details = tourDetailsRepository.findByProgrammeId(programme.getId())
                .orElseThrow(() -> new IllegalStateException(
                        "Programme %d has no campus tour details".formatted(programme.getId())));

        Domain hospitality = domainRepository.findByCode(HOSPITALITY_LOGISTICS)
                .orElseThrow(() -> new IllegalStateException("Domain " + HOSPITALITY_LOGISTICS + " not seeded"));
        Domain vcu = domainRepository.findByCode(VCU)
                .orElseThrow(() -> new IllegalStateException("Domain " + VCU + " not seeded"));

        if (details.isLunchRequired()) {
            createIfAbsent(programme, hospitality, RequirementKind.LUNCH, dueDate);
        }
        if (tourInterestRepository.anyRequiresApproval(programme.getId())) {
            createIfAbsent(programme, hospitality, RequirementKind.VENUE_APPROVALS, dueDate);
        }
        // Volunteers (including the escort slot) are always needed for a tour.
        createIfAbsent(programme, vcu, RequirementKind.VOLUNTEERS, dueDate);
    }

    private void createIfAbsent(Programme programme, Domain domain, RequirementKind kind, LocalDate dueDate) {
        requirementRepository.findByProgrammeIdAndKind(programme.getId(), kind)
                .orElseGet(() -> requirementRepository.save(new Requirement(programme, domain, kind, dueDate)));
    }

    private Programme reject(Programme programme, DecisionRequest request, AppUser actor) {
        programme.transitionTo(ProgrammeStatus.REJECTED);
        programme.setDecidedBy(actor);
        programme.setDecidedAt(Instant.now());
        programme.setDecisionNote(request.note());

        auditLogRepository.save(AuditLog.byUser(actor.getId(), "TOUR_REJECTED", "Programme",
                programme.getId(), Map.of("note", Optional.ofNullable(request.note()).orElse(""))));
        return programme;
    }

    private Programme proposeDate(Programme programme, DecisionRequest request, AppUser actor) {
        if (request.proposedDate() == null) {
            throw new IllegalArgumentException("proposedDate is required for PROPOSE_DATE");
        }
        programme.transitionTo(ProgrammeStatus.RESCHEDULE_PROPOSED);
        programme.setProposedDate(request.proposedDate());
        programme.setDecisionNote(request.note());

        auditLogRepository.save(AuditLog.byUser(actor.getId(), "TOUR_RESCHEDULE_PROPOSED", "Programme",
                programme.getId(), Map.of("proposedDate", request.proposedDate().toString())));
        return programme;
    }

    /**
     * Called when a school or staff member revises headcount for an already
     * approved tour: flags the domains that already acted so they notice
     * the change instead of silently working off stale numbers.
     */
    @Transactional
    public void flagRequirementsForHeadcountChange(Long programmeId) {
        for (Requirement requirement : requirementRepository.findByProgrammeId(programmeId)) {
            if (requirement.getKind() == RequirementKind.LUNCH
                    || requirement.getKind() == RequirementKind.VOLUNTEERS) {
                requirement.markNeedsAttention();
            }
        }
    }
}