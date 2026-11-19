-- A school asking for a different date no longer changes the tour directly.
-- The ask is parked in these columns until staff approve or decline it.
ALTER TABLE programme
    ADD COLUMN requested_visit_date     DATE,
    ADD COLUMN requested_arrival_time   TIME,
    ADD COLUMN requested_departure_time TIME,
    ADD COLUMN date_change_note         TEXT;   -- why staff declined the last request (shown to the school)
