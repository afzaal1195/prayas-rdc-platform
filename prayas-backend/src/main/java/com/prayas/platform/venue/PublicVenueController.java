package com.prayas.platform.venue;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/public/venues")
public class PublicVenueController {

    private final VenueRepository venueRepository;

    public PublicVenueController(VenueRepository venueRepository) {
        this.venueRepository = venueRepository;
    }

    @GetMapping
    public List<VenueView> list() {
        return venueRepository.findByPublicVisibleTrueAndActiveTrue()
                .stream().map(VenueView::from).toList();
    }
}
