package com.prayas.platform.itinerary;

import com.prayas.platform.itinerary.ItineraryViews.PublicStop;
import com.prayas.platform.tour.CampusTourDetailsRepository;
import com.prayas.platform.tour.TrackingTokenGenerator;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

/** The school's own view of its itinerary, found by the secret tracking token (same as the status page). */
@RestController
@RequestMapping("/api/v1/public")
public class PublicItineraryController {

    private final ItineraryService service;
    private final CampusTourDetailsRepository tourDetails;
    private final TrackingTokenGenerator tokenGenerator;

    public PublicItineraryController(ItineraryService service,
                                     CampusTourDetailsRepository tourDetails,
                                     TrackingTokenGenerator tokenGenerator) {
        this.service = service;
        this.tourDetails = tourDetails;
        this.tokenGenerator = tokenGenerator;
    }

    @GetMapping("/tour-requests/{token}/itinerary")
    public List<PublicStop> itinerary(@PathVariable String token) {
        Long programmeId = tourDetails.findByTrackingTokenHash(tokenGenerator.hash(token))
                .orElseThrow(() -> new NoSuchElementException("No tour request for this link"))
                .getProgramme().getId();
        return service.forSchool(programmeId);
    }
}
