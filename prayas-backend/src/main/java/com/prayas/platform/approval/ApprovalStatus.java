package com.prayas.platform.approval;

public enum ApprovalStatus {
    /** Not open yet: an earlier step in the chain has to be approved first. Set by the system only. */
    WAITING,
    /** Open, nobody has asked the authority yet. */
    PENDING,
    /** The authority has been contacted; waiting for their answer. */
    REQUESTED,
    APPROVED,
    DENIED
}
