package com.nightout.service;

import com.nightout.dto.EventDto;
import com.nightout.mapper.EventMapper;
import com.nightout.model.Event;
import com.nightout.repository.EventRepo;
import com.nightout.repository.VenueRepo;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class EventService {
    private static final LocalTime NIGHT_START=LocalTime.of(18, 0);
    private static final LocalTime NIGHT_END=LocalTime.of(6, 0);

    private final EventRepo eventRepository;
    private final VenueRepo venueRepository;
    private final EventMapper eventMapper;

    public EventService(EventRepo eventRepository,
                        VenueRepo venueRepository,
                        EventMapper eventMapper) {
        this.eventRepository = eventRepository;
        this.venueRepository = venueRepository;
        this.eventMapper = eventMapper;
    }

    public List<EventDto> findTonight(String city) {
        LocalDateTime now=LocalDateTime.now();

        LocalDate nightDate=now.toLocalTime().isBefore(NIGHT_END)
                ? now.toLocalDate().minusDays(1)
                : now.toLocalDate();

        LocalDateTime from=nightDate.atTime(NIGHT_START);
        LocalDateTime to=nightDate.plusDays(1).atTime(NIGHT_END);

        return eventRepository
                .findByVenueCityNameIgnoreCaseAndStartTimeBetweenOrderByStartTime(city, from, to)
                .stream()
                .map(eventMapper::toDto)
                .toList();
    }

    public List<EventDto> findByVenue(Long venueId) {
        if (!venueRepository.existsById(venueId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Venue " + venueId + " not found");
        }
        return eventRepository.findByVenueId(venueId).stream()
                .map(eventMapper::toDto)
                .toList();
    }

    public EventDto findById(Long id) {
        Event event = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Event " + id + " not found"));
        return eventMapper.toDto(event);
    }
}
