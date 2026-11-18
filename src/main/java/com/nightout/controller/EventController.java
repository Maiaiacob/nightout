package com.nightout.controller;

import com.nightout.dto.EventDto;
import com.nightout.service.EventService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
public class EventController {
    private final EventService eventService;

    public EventController(EventService eventService) {
        this.eventService = eventService;
    }

    @GetMapping("/tonight")
    public List<EventDto> getEventsTonight(@RequestParam String city) {
        return eventService.findTonight(city);
    }

    @GetMapping
    public List<EventDto> getEventsByVenue(@RequestParam Long venueId) {
        return eventService.findByVenue(venueId);
    }

    @GetMapping("/{id}")
    public EventDto getEvent(@PathVariable Long id) {
        return eventService.findById(id);
    }
}
