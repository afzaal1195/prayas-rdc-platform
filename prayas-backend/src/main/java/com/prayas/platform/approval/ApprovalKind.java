package com.prayas.platform.approval;

/** What a step is asking permission for. */
public enum ApprovalKind {
    /** A venue the school chose that needs someone's say-so (a lab, a classroom). */
    VENUE,
    /** Lunch, step 1: the hostel office agrees to host the group. */
    HOSTEL_OFFICE,
    /** Lunch, step 2: the mess in-charge confirms the meal counts. Opens only after step 1 is approved. */
    MESS
}
