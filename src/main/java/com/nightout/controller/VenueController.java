package com.nightout.controller;

import com.nightout.dto.NearbyVenueDto;
import com.nightout.dto.VenueDto;
import com.nightout.model.VenueType;
import com.nightout.service.VenueService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/venues")
public class VenueController {
    private final VenueService venueService;

    public VenueController(VenueService venueService) {
        this.venueService = venueService;
    }

    @GetMapping
    public List<VenueDto> getVenues(@RequestParam String city,
                                    @RequestParam(required = false) VenueType type) {
        return venueService.findByCity(city, type);
    }

    @GetMapping("/nearby")
    public List<NearbyVenueDto> getNearby(@RequestParam double lat,
                                          @RequestParam double lng,
                                          @RequestParam(defaultValue = "1000") int radius,
                                          @RequestParam(defaultValue = "false") boolean open) {
        return venueService.findNearby(lat, lng, radius, open);
    }

    @GetMapping("/{id}")
    public VenueDto getVenue(@PathVariable Long id) {
        return venueService.findById(id);
    }
}
