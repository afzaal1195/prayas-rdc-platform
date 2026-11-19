CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE tour_assignment (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id BIGINT      NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    user_id      BIGINT      NOT NULL REFERENCES app_user (id),
    role         VARCHAR(15) NOT NULL CHECK (role IN ('COORDINATOR', 'VOLUNTEER')),
    starts_at    TIMESTAMPTZ NOT NULL,
    ends_at      TIMESTAMPTZ NOT NULL,
    status       VARCHAR(15) NOT NULL DEFAULT 'ACTIVE' CHECK (status IN ('ACTIVE', 'RELEASED')),
    assigned_by  BIGINT REFERENCES app_user (id),
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    CHECK (ends_at > starts_at),
    CONSTRAINT no_overlapping_shifts
        EXCLUDE USING gist (user_id WITH =, tstzrange(starts_at, ends_at) WITH &&)
        WHERE (status = 'ACTIVE')
);
CREATE INDEX idx_assignment_programme ON tour_assignment (programme_id, status);
