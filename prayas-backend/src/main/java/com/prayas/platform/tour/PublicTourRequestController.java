package com.prayas.platform.tour;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Reachable with no login. Every write here goes through the honeypot,
 * captcha check, and per-IP rate limit before touching the database --
 * see PublicTourRequestService and PublicSubmitRateLimitFilter. The
 * tracking token itself (see TrackingTokenGenerator) is what stands in
 * for authentication on every endpoint below /tour-requests/{token}/**.
 */
@RestController
@RequestMapping("/api/v1/public")
public class PublicTourRequestController {

    private final PublicTourRequestService service;

    public PublicTourRequestController(PublicTourRequestService service) {
        this.service = service;
    }

    @PostMapping("/tour-requests")
    public TourRequestCreated submit(@Valid @RequestBody TourRequestCreate request,
                                      HttpServletRequest httpRequest) {
        return service.submit(request, httpRequest.getRemoteAddr());
    }

    @GetMapping("/tour-requests/{token}")
    public TourStatusView status(@PathVariable String token) {
        return service.status(token);
    }

    // Only one of grades/courses is populated, matching whichever
    // institutionType the request was originally submitted as -- the
    // total student count is derived from these, same as a fresh submission.
    public record HeadcountUpdate(@Valid List<TourRequestCreate.GradeCount> grades,
                                   @Valid List<TourRequestCreate.CourseCount> courses,
                                   @Min(0) int teacherCount,
                                   boolean lunchRequired,
                                   Integer mealCount) {
    }

    @PostMapping("/tour-requests/{token}/headcount")
    public void reviseHeadcount(@PathVariable String token, @Valid @RequestBody HeadcountUpdate update) {
        service.reviseHeadcount(token, update.grades(), update.courses(), update.teacherCount(),
                update.lunchRequired(), update.mealCount());
    }

    public record CancelRequest(@Size(max = 500) String reason) {
    }

    @PostMapping("/tour-requests/{token}/cancel")
    public void cancel(@PathVariable String token, @RequestBody(required = false) CancelRequest request) {
        service.cancel(token, request != null ? request.reason() : null);
    }

    public record RescheduleRequest(@NotNull @Future LocalDate newVisitDate,
                                     LocalTime newArrivalTime, LocalTime newDepartureTime) {
    }

    @PostMapping("/tour-requests/{token}/reschedule")
    public void requestDateChange(@PathVariable String token, @Valid @RequestBody RescheduleRequest request) {
        service.requestDateChange(token, request.newVisitDate(), request.newArrivalTime(), request.newDepartureTime());
    }
}