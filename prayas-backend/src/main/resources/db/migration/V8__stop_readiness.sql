-- =====================================================================
-- Readiness tracking: is each itinerary stop prepared? NOT_READY until
-- someone marks it READY, or BLOCKED (with a note saying what is wrong).
-- =====================================================================

ALTER TABLE itinerary_stop
    ADD COLUMN readiness            VARCHAR(12) NOT NULL DEFAULT 'NOT_READY'
        CHECK (readiness IN ('NOT_READY', 'READY', 'BLOCKED')),
    ADD COLUMN readiness_note       TEXT,
    ADD COLUMN readiness_updated_by BIGINT REFERENCES app_user (id),
    ADD COLUMN readiness_updated_at TIMESTAMPTZ;
