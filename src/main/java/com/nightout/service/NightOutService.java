package com.nightout.service;

import com.nightout.dto.NightOutDto;
import com.nightout.hours.OpeningHoursService;
import com.nightout.mapper.CityMapper;
import com.nightout.mapper.EventMapper;
import com.nightout.mapper.VenueMapper;
import com.nightout.model.City;
import com.nightout.model.Event;
import com.nightout.repository.CityRepo;
import com.nightout.repository.EventRepo;
import com.nightout.repository.VenueRepo;
import com.nightout.util.Night;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;

@Service
public class NightOutService {

    private final CityRepo cityRepository;
    private final VenueRepo venueRepository;
    private final EventRepo eventRepository;
    private final ImportService importService;
    private final CityMapper cityMapper;
    private final EventMapper eventMapper;
    private final VenueMapper venueMapper;
    private final VenueImporter venueImporter;
    private final OpeningHoursService openingHoursService;

    public NightOutService(CityRepo cityRepository, VenueRepo venueRepository,
                           EventRepo eventRepository, ImportService importService,
                           CityMapper cityMapper, EventMapper eventMapper, VenueMapper venueMapper,
                           VenueImporter venueImporter, OpeningHoursService openingHoursService) {
        this.cityRepository = cityRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.importService = importService;
        this.cityMapper = cityMapper;
        this.eventMapper = eventMapper;
        this.venueMapper = venueMapper;
        this.venueImporter = venueImporter;
        this.openingHoursService = openingHoursService;
    }

    @Transactional
    public NightOutDto search(String cityName) {

        String name = cityName.trim();
        City city = cityRepository.findByNameIgnoreCase(name)
                .filter(c -> c.getLatitude() != null)
                .orElseGet(() -> importService.importCity(name));

        venueImporter.startIfNeeded(city);

        if (!Night.current().equals(city.getEventsUpdatedOn())) {
            importService.refreshEvents(city);
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDate nightDate = Night.current();
        LocalDateTime nightStart = nightDate.atTime(16, 0);
        LocalDateTime nightEnd = nightDate.plusDays(1).atTime(6, 0);
        LocalDateTime from = now.minusHours(3).isAfter(nightStart) ? now.minusHours(3) : nightStart;

        var events = eventRepository
                .findByVenueCityNameIgnoreCaseAndStartTimeBetweenOrderByStartTime(city.getName(), from, nightEnd)
                .stream()
                .map(eventMapper::toDto)
                .toList();

        var cityVenues = venueRepository.findByCityNameIgnoreCase(city.getName());
        var openNow = openingHoursService.openNow(cityVenues);
        var venues = cityVenues.stream()
                .map(v -> venueMapper.toDto(v, openNow.get(v.getId())))
                .toList();

        return new NightOutDto(cityMapper.toDto(city), events, venues, venueImporter.isLoading(city));
    }
}