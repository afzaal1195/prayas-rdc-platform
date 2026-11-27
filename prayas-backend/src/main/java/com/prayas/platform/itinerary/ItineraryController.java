package com.prayas.platform.itinerary;

import com.prayas.platform.auth.CurrentUser;
import com.prayas.platform.itinerary.ItineraryViews.Board;
import com.prayas.platform.user.AppUser;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Planning itineraries: who may is decided in AccessService.canPlanItineraries; the rules about stops are in ItineraryService. */
@RestController
@RequestMapping("/api/v1")
public class ItineraryController {

    private final ItineraryService service;

    public ItineraryController(ItineraryService service) {
        this.service = service;
    }

    @GetMapping("/itineraries")
    @PreAuthorize("@access.canPlanItineraries()")
    public Board itineraries() {
        return service.board();
    }

    @PostMapping("/tour-requests/{programmeId}/itinerary")
    @PreAuthorize("@access.canPlanItineraries()")
    @ResponseStatus(HttpStatus.CREATED)
    public void addStop(@PathVariable Long programmeId,
                        @Valid @RequestBody AddStopRequest request,
                        @CurrentUser AppUser actor) {
        service.addStop(programmeId, request, actor);
    }

    @DeleteMapping("/itinerary/{stopId}")
    @PreAuthorize("@access.canPlanItineraries()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStop(@PathVariable Long stopId, @CurrentUser AppUser actor) {
        service.removeStop(stopId, actor);
    }
}
