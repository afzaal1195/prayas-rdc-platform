-- =====================================================================
-- Phase 2, milestone 1: authority directory
--
-- Someone outside Prayas has to say yes before a lab visit, a classroom
-- booking or the mess lunch can go ahead. This table is the directory a
-- volunteer works from when they pick up one of those tasks: who to
-- contact, how, and when they're reachable.
--
-- It is deliberately NOT tied to a venue table row directly: the hostel
-- office and the mess in-charge (the lunch chain, a later milestone) are
-- authorities too, but they are not venues. A venue just points at the
-- contact responsible for approving it.
-- =====================================================================

CREATE TABLE authority_contact (
    id                BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    name              VARCHAR(150) NOT NULL,   -- a person or an office, e.g. "Head, Dept. of CSE"
    office            VARCHAR(150),            -- e.g. "Department of Computer Science"
    phone             VARCHAR(30),
    email             VARCHAR(255),
    office_hours      VARCHAR(150),            -- free text, e.g. "Mon-Fri 10:00-17:00"
    preferred_contact VARCHAR(30),             -- free text, e.g. "Phone", "In person", "Email"
    notes             TEXT,
    is_active         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);

ALTER TABLE venue
    ADD COLUMN authority_contact_id BIGINT REFERENCES authority_contact (id);

CREATE INDEX idx_venue_authority ON venue (authority_contact_id);
