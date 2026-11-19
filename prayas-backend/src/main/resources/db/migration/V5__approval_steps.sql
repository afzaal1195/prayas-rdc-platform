-- =====================================================================
-- Approval steps: the outside permissions a tour needs before it can run.
--
--   VENUE         one per chosen venue that needs approval (a lab, a classroom)
--   HOSTEL_OFFICE first step of the lunch chain
--   MESS          second step of the lunch chain; stays WAITING until the
--                 hostel office has said yes
--
-- Steps are created when a tour is approved. The status of the matching
-- requirement (VENUE_APPROVALS, LUNCH) is worked out from them.
-- =====================================================================

CREATE TABLE approval_step (
    id                   BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id         BIGINT      NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    kind                 VARCHAR(20) NOT NULL CHECK (kind IN ('VENUE', 'HOSTEL_OFFICE', 'MESS')),
    venue_id             BIGINT REFERENCES venue (id),
    authority_contact_id BIGINT REFERENCES authority_contact (id),
    sequence             INT         NOT NULL DEFAULT 1,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING'
                         CHECK (status IN ('WAITING', 'PENDING', 'REQUESTED', 'APPROVED', 'DENIED')),
    note                 TEXT,
    decided_by           BIGINT REFERENCES app_user (id),
    decided_at           TIMESTAMPTZ,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK ((kind = 'VENUE') = (venue_id IS NOT NULL))
);

-- One step per kind per tour, and one VENUE step per venue.
CREATE UNIQUE INDEX uq_approval_step ON approval_step (programme_id, kind, COALESCE(venue_id, 0));
CREATE INDEX idx_approval_step_status ON approval_step (status);
