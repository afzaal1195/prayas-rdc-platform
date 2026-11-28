package com.prayas.platform.itinerary;

/** Whether an itinerary stop is prepared. BLOCKED always comes with a note saying what is in the way. */
public enum StopReadiness {
    NOT_READY,
    READY,
    BLOCKED
}
