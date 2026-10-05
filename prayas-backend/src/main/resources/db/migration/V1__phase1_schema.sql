-- =====================================================================
-- Prayas Campus Tour Platform -- Phase 1 schema
-- Target: PostgreSQL 15+, applied with Flyway (V1__phase1_schema.sql)
-- Scope : school request form, tour review/approval, auto-created
--         domain requirements, audit trail.
-- Later : V2 workflow templates/steps (approvals, mess chain),
--         V3 availability/assignments/shifts (needs btree_gist),
--         V4 check-ins/media.
-- Conventions: text + CHECK instead of PG enums (easier to evolve),
--              timestamptz everywhere, ids are identity columns.
-- =====================================================================

-- ---------- People & org structure -----------------------------------

CREATE TABLE app_user (
    id                       BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    email                    VARCHAR(255) NOT NULL,
    full_name                VARCHAR(150) NOT NULL,
    phone                    VARCHAR(20),
    -- Org-wide role. Domain-specific positions live in domain_membership.
    global_role              VARCHAR(20)  NOT NULL DEFAULT 'MEMBER'
                             CHECK (global_role IN ('FACULTY_INCHARGE', 'LEAD', 'MEMBER')),
    -- Coordinators who may be allocated to volunteer slots when volunteers are short.
    can_fill_volunteer_slots BOOLEAN      NOT NULL DEFAULT FALSE,
    is_active                BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at               TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE UNIQUE INDEX uq_app_user_email ON app_user (lower(email));

CREATE TABLE domain (
    id   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    code VARCHAR(30)  NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL UNIQUE
);

INSERT INTO domain (code, name) VALUES
    ('PR_OUTREACH',           'PR & Outreach'),
    ('EVENTS',                'Events'),
    ('HOSPITALITY_LOGISTICS', 'Hospitality & Logistics'),
    ('TRANSPORT',             'Transport'),
    ('VCU',                   'Volunteer Coordination Unit'),
    ('EDU_PROGRAMS',          'Educational Program Management'),
    ('WEB',                   'Web'),
    ('MULTIMEDIA',            'Multimedia'),
    ('CAMPUS_TOUR',           'Campus Tour'),
    ('FINANCE',               'Finance');

CREATE TABLE domain_membership (
    user_id     BIGINT      NOT NULL REFERENCES app_user (id) ON DELETE CASCADE,
    domain_id   BIGINT      NOT NULL REFERENCES domain (id),
    domain_role VARCHAR(15) NOT NULL
                CHECK (domain_role IN ('HEAD', 'COORDINATOR', 'VOLUNTEER')),
    joined_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, domain_id)
);
CREATE INDEX idx_membership_domain ON domain_membership (domain_id);

-- ---------- Schools & venues ------------------------------------------

CREATE TABLE school (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name            VARCHAR(200) NOT NULL,
    address         TEXT,
    village_or_town VARCHAR(120),
    district        VARCHAR(120),
    state           VARCHAR(80),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_school_name ON school (lower(name));

CREATE TABLE venue (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,
    venue_type        VARCHAR(20)  NOT NULL
                      CHECK (venue_type IN ('LEGACY_ROOM', 'LIBRARY', 'LAB', 'SPORTS', 'CLASSROOM', 'OTHER')),
    department        VARCHAR(120),
    capacity          INT CHECK (capacity > 0),
    -- Phase 2 uses this to spawn approval workflows automatically.
    requires_approval BOOLEAN      NOT NULL DEFAULT FALSE,
    -- Whether schools can pick this venue on the public form.
    public_visible    BOOLEAN      NOT NULL DEFAULT TRUE,
    description       TEXT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE
);

-- Sample rows; adjust requires_approval to match reality. Labs are added by admins.
INSERT INTO venue (name, venue_type, requires_approval) VALUES
    ('Legacy Room',    'LEGACY_ROOM', FALSE),
    ('Library',        'LIBRARY',     FALSE),
    ('Sports Complex', 'SPORTS',      FALSE);

-- ---------- Programmes (generic) + campus tour specifics ---------------

CREATE TABLE programme (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_type VARCHAR(30) NOT NULL CHECK (programme_type IN ('CAMPUS_TOUR')),
    status         VARCHAR(30) NOT NULL DEFAULT 'SUBMITTED'
                   CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'RESCHEDULE_PROPOSED',
                                     'APPROVED', 'REJECTED', 'CANCELLED', 'COMPLETED')),
    visit_date     DATE        NOT NULL,
    arrival_time   TIME,
    departure_time TIME,
    proposed_date  DATE,                       -- set when a lead proposes another date
    decided_by     BIGINT REFERENCES app_user (id),
    decided_at     TIMESTAMPTZ,
    decision_note  TEXT,
    version        INT         NOT NULL DEFAULT 0,   -- JPA @Version, optimistic locking
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (departure_time IS NULL OR arrival_time IS NULL OR departure_time > arrival_time)
);
CREATE INDEX idx_programme_date_status ON programme (visit_date, status);

CREATE TABLE campus_tour_details (
    programme_id        BIGINT PRIMARY KEY REFERENCES programme (id) ON DELETE CASCADE,
    school_id           BIGINT       NOT NULL REFERENCES school (id),
    institution_type    VARCHAR(20)  NOT NULL DEFAULT 'SCHOOL',  -- SCHOOL or COLLEGE
    contact_name        VARCHAR(150) NOT NULL,
    contact_phone       VARCHAR(20)  NOT NULL,
    contact_email       VARCHAR(255) NOT NULL,
    grade_range         VARCHAR(500),          -- school: e.g. "Grade 8: 20, Grade 9: 15"; college: e.g. "B.Tech CSE (2nd Year): 20"
    vehicle_number      VARCHAR(30),           -- for campus entry, if security needs it
    arrival_point       VARCHAR(150),
    lunch_required      BOOLEAN      NOT NULL DEFAULT FALSE,
    interest_notes      TEXT,                  -- free text: what they want to see and why
    special_needs       TEXT,
    -- Only the SHA-256 hash of the tracking token is stored; the raw token is
    -- shown to the school once (response + email) and never persisted.
    tracking_token_hash VARCHAR(64) NOT NULL UNIQUE
);

-- Accompanying school teachers / coordinators
CREATE TABLE tour_teacher (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id BIGINT       NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    full_name    VARCHAR(150) NOT NULL,
    phone        VARCHAR(20)  NOT NULL
);
CREATE INDEX idx_tour_teacher_programme ON tour_teacher (programme_id);

-- What the school wants to see (picked from public venues)
CREATE TABLE tour_interest (
    programme_id BIGINT NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    venue_id     BIGINT NOT NULL REFERENCES venue (id),
    PRIMARY KEY (programme_id, venue_id)
);

-- Headcount is versioned: the latest revision is current, older ones are history.
-- Revision 1 is created from the request form.
CREATE TABLE headcount_revision (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id  BIGINT      NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    revision_no   INT         NOT NULL,
    student_count INT         NOT NULL CHECK (student_count > 0),
    teacher_count INT         NOT NULL DEFAULT 0 CHECK (teacher_count >= 0),
    veg_meals     INT CHECK (veg_meals >= 0),
    non_veg_meals INT CHECK (non_veg_meals >= 0),
    reason        TEXT,
    changed_by    BIGINT REFERENCES app_user (id),   -- NULL when the school changed it
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (programme_id, revision_no)
);

CREATE VIEW current_headcount AS
SELECT DISTINCT ON (programme_id)
       programme_id, revision_no, student_count, teacher_count,
       veg_meals, non_veg_meals, created_at
FROM headcount_revision
ORDER BY programme_id, revision_no DESC;

-- ---------- Domain requirements (spawned when a tour is approved) ------

CREATE TABLE requirement (
    id             BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id   BIGINT      NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    domain_id      BIGINT      NOT NULL REFERENCES domain (id),
    kind           VARCHAR(30) NOT NULL
                   CHECK (kind IN ('LUNCH', 'VOLUNTEERS', 'VENUE_APPROVALS')),
    status         VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                   CHECK (status IN ('PENDING', 'IN_PROGRESS', 'DONE', 'BLOCKED', 'CANCELLED')),
    -- Raised when something changes after the domain acted (e.g. headcount revised).
    needs_attention BOOLEAN    NOT NULL DEFAULT FALSE,
    payload        JSONB       NOT NULL DEFAULT '{}'::jsonb,
    due_date       DATE,
    updated_by     BIGINT REFERENCES app_user (id),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (programme_id, kind)
);
CREATE INDEX idx_requirement_domain_status ON requirement (domain_id, status);

-- ---------- Audit trail ------------------------------------------------

CREATE TABLE audit_log (
    id            BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    actor_user_id BIGINT REFERENCES app_user (id),   -- NULL for public/school actions
    actor_label   VARCHAR(100),                      -- e.g. 'school-via-token'
    action        VARCHAR(60) NOT NULL,              -- e.g. TOUR_APPROVED
    entity_type   VARCHAR(40) NOT NULL,
    entity_id     BIGINT,
    details       JSONB,
    created_at    TIMESTAMPTZ NOT NULL DEFAULT now()
);
CREATE INDEX idx_audit_entity ON audit_log (entity_type, entity_id);
