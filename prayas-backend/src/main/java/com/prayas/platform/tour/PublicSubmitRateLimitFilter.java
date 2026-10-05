package com.prayas.platform.tour;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-IP submission cap on the public tour-request endpoint, configurable
 * via prayas.tour.rate-limit-per-hour (defaults to 5, the real production
 * value -- local dev overrides it higher in application-local.yml so
 * repeated manual testing doesn't get blocked). A simple in-memory bucket
 * is enough for a single-instance deployment; move to a Redis-backed
 * Bucket4j proxy manager if this ever runs on more than one node.
 */
public class PublicSubmitRateLimitFilter extends OncePerRequestFilter {

    private static final String PROTECTED_PATH = "/api/v1/public/tour-requests";
    private static final DateTimeFormatter RETRY_TIME_FORMAT = DateTimeFormatter.ofPattern("h:mm a");

    private final ConcurrentMap<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final int limitPerHour;

    public PublicSubmitRateLimitFilter(int limitPerHour) {
        this.limitPerHour = limitPerHour;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {
        boolean isProtectedPost = "POST".equalsIgnoreCase(request.getMethod())
                && PROTECTED_PATH.equals(request.getRequestURI());

        if (isProtectedPost) {
            Bucket bucket = buckets.computeIfAbsent(clientIp(request), ip ->
                    Bucket.builder()
                            .addLimit(Bandwidth.simple(limitPerHour, Duration.ofHours(1)))
                            .build());
            ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);
            if (!probe.isConsumed()) {
                Duration waitTime = Duration.ofNanos(probe.getNanosToWaitForRefill());
                String retryAt = LocalTime.now().plus(waitTime).format(RETRY_TIME_FORMAT);
                response.setStatus(429);
                response.setContentType("application/json");
                response.getWriter().write(
                        "{\"detail\":\"Too many requests. Please try again at " + retryAt + ".\"}");
                return;
            }
        }
        chain.doFilter(request, response);
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        return forwarded != null ? forwarded.split(",")[0].trim() : request.getRemoteAddr();
    }
}