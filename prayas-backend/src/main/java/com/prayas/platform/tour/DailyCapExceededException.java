package com.prayas.platform.tour;

/** Thrown when approving a tour would exceed the configured daily cap. Mapped to HTTP 409. */
public class DailyCapExceededException extends RuntimeException {
    public DailyCapExceededException(String message) {
        super(message);
    }
}
