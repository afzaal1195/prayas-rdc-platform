package com.prayas.platform.tour;

/** Thrown when a school sends a new request while one of its earlier requests is still waiting for a decision. Mapped to HTTP 409. */
public class DuplicateOpenRequestException extends RuntimeException {
    public DuplicateOpenRequestException(String message) {
        super(message);
    }
}