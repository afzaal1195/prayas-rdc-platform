-- =====================================================================
-- Chain of command: who looks after each domain's jobs for each school.
--
-- Faculty in-charge and leads (and domain heads) put a COORDINATOR on a
-- domain's work for a tour; the coordinator then puts domain VOLUNTEERS
-- on it. A domain can have several coordinators and volunteers on one
-- tour. This is separate from tour_assignment (V3), which is who covers
-- which hours of the visit day.
-- =====================================================================

CREATE TABLE domain_assignment (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id BIGINT      NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    domain_id    BIGINT      NOT NULL REFERENCES domain (id),
    user_id      BIGINT      NOT NULL REFERENCES app_user (id),
    role         VARCHAR(15) NOT NULL CHECK (role IN ('COORDINATOR', 'VOLUNTEER')),
    assigned_by  BIGINT REFERENCES app_user (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    -- a person is on a tour's domain team once, as either a coordinator or a volunteer
    UNIQUE (programme_id, domain_id, user_id)
);

CREATE INDEX idx_domain_assignment_user ON domain_assignment (user_id);
CREATE INDEX idx_domain_assignment_programme ON domain_assignment (programme_id);
