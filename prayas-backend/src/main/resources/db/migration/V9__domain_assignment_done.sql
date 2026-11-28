-- =====================================================================
-- "My tasks": a person on a domain's team can tick off their own part of
-- a tour, with an optional note and an optional proof link (e.g. a photo
-- in a shared folder). Cleared again if the tour moves to a new date.
-- =====================================================================

ALTER TABLE domain_assignment
    ADD COLUMN completed_at    TIMESTAMPTZ,
    ADD COLUMN completion_note VARCHAR(500),
    ADD COLUMN proof_url       VARCHAR(500);
