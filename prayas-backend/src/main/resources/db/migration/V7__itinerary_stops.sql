-- =====================================================================
-- Itinerary: the ordered stops of an approved tour (where the group is,
-- and when). A stop is either a campus venue or a free-text activity
-- (a welcome talk, lunch). The rules about the visit window and about two
-- schools using the same venue at the same time are enforced in
-- ItineraryService; they depend on other tables, so a CHECK can't hold them.
-- =====================================================================

CREATE TABLE itinerary_stop (
    id           BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    programme_id BIGINT       NOT NULL REFERENCES programme (id) ON DELETE CASCADE,
    venue_id     BIGINT REFERENCES venue (id),
    title        VARCHAR(150) NOT NULL,
    start_time   TIME         NOT NULL,
    end_time     TIME         NOT NULL,
    notes        TEXT,
    created_by   BIGINT REFERENCES app_user (id),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CHECK (end_time > start_time)
);

CREATE INDEX idx_itinerary_stop_programme ON itinerary_stop (programme_id);
CREATE INDEX idx_itinerary_stop_venue ON itinerary_stop (venue_id) WHERE venue_id IS NOT NULL;
