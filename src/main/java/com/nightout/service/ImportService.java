package com.nightout.service;

import com.nightout.client.NominatimClient;
import com.nightout.client.OverpassClient;
import com.nightout.client.TicketmasterClient;
import com.nightout.client.dto.NominatimPlace;
import com.nightout.client.dto.OverpassResponse;
import com.nightout.client.dto.TicketmasterResponse;
import com.nightout.hours.OpeningHoursService;
import com.nightout.model.*;
import com.nightout.repository.CityRepo;
import com.nightout.repository.EventRepo;
import com.nightout.repository.VenueRepo;
import com.nightout.util.Night;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class ImportService {

    private static final Logger log = LoggerFactory.getLogger(ImportService.class);

    private final NominatimClient nominatimClient;
    private final OverpassClient overpassClient;
    private final TicketmasterClient ticketmasterClient;
    private final CityRepo cityRepository;
    private final VenueRepo venueRepository;
    private final EventRepo eventRepository;
    private final OpeningHoursService openingHoursService;

    public ImportService(NominatimClient nominatimClient, OverpassClient overpassClient,
                         TicketmasterClient ticketmasterClient, CityRepo cityRepository,
                         VenueRepo venueRepository, EventRepo eventRepository,
                         OpeningHoursService openingHoursService) {
        this.nominatimClient = nominatimClient;
        this.overpassClient = overpassClient;
        this.ticketmasterClient = ticketmasterClient;
        this.cityRepository = cityRepository;
        this.venueRepository = venueRepository;
        this.eventRepository = eventRepository;
        this.openingHoursService = openingHoursService;
    }

    @Transactional
    public City importCity(String cityName) {
        long started=System.currentTimeMillis();
        NominatimPlace place=nominatimClient.findCity(cityName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "City not found: " + cityName));

        //missing bars are loaded in the background by VenueImporter.
        Optional<City> known=cityRepository.findByNameIgnoreCase(place.name())
                .filter(c -> c.getLatitude() != null);
        if (known.isPresent()) {
            return known.get();
        }

        City city=cityRepository.findByNameIgnoreCase(place.name())
                .orElseGet(() -> {
                    String country = place.address() != null
                            ? place.address().getOrDefault("country", "Unknown")
                            : "Unknown";
                    return cityRepository.save(new City(place.name(), country));
                });

        double lat=Double.parseDouble(place.lat());
        double lon=Double.parseDouble(place.lon());
        city.setLatitude(lat);
        city.setLongitude(lon);
        saveEvents(city, ticketmasterClient.findEvents(lat, lon, 1));
        city.setEventsUpdatedOn(Night.current());
        cityRepository.save(city);
        log.info("Imported {} in {} ms", city.getName(), System.currentTimeMillis() - started);
        return city;
    }

    /** Save bars for a city */
    @Transactional
    public void saveVenues(Long cityId, List<OverpassResponse.Element> elements) {
        City city=cityRepository.findById(cityId).orElseThrow();
        for (OverpassResponse.Element element : elements) {
            Map<String, String> tags=element.tags();
            if (tags==null || venueRepository.existsByOsmId(element.id())) continue;
            VenueType type = toVenueType(tags.get("amenity"));
            if (type==null) continue;
            Address address=new Address(tags.get("addr:street"), tags.get("addr:housenumber"),
                    tags.get("addr:postcode"), element.lat(), element.lon());

            Venue venue=new Venue(tags.get("name"), tags.get("description"), address, type, city);
            venue.setOsmId(element.id());
            venue.setOpeningHours(tags.get("opening_hours"));
            venue.setImageUrl(toImageUrl(tags));
            venue.setWikidata(tags.get("wikidata"));
            openingHoursService.apply(venueRepository.save(venue));
        }
    }

    private void importEvents(City city) {
        saveEvents(city, ticketmasterClient.findEvents(city.getLatitude(), city.getLongitude(), 1));
    }

    private void saveEvents(City city, List<TicketmasterResponse.TmEvent> tmEvents) {
        for (TicketmasterResponse.TmEvent tmEvent : tmEvents) {
            if (tmEvent.id()==null || eventRepository.existsByExternalId(tmEvent.id())) continue;

            LocalDateTime start=toStart(tmEvent);
            TicketmasterResponse.TmVenue tmVenue=tmEvent.embedded()!=null
                    && tmEvent.embedded().venues()!=null && !tmEvent.embedded().venues().isEmpty()
                    ? tmEvent.embedded().venues().get(0) : null;
            Venue venue=findOrCreateVenue(city, tmVenue);
            if (start==null || venue==null) continue;

            Event event=new Event(tmEvent.name(), null, start, null, null, venue);
            event.setExternalId(tmEvent.id());
            event.setUrl(tmEvent.url());
            event.setImageUrl(pickImage(tmEvent.images()));
            if (tmEvent.classifications()!=null && !tmEvent.classifications().isEmpty()) {
                TicketmasterResponse.Classification c=tmEvent.classifications().get(0);
                event.setCategory(cleanName(c.segment()));
                event.setGenre(cleanName(c.genre()));
            }
            eventRepository.save(event);
        }
    }

    private Venue findOrCreateVenue(City city, TicketmasterResponse.TmVenue tmVenue) {
        if (tmVenue==null || tmVenue.name()==null) return null;
        return venueRepository.findFirstByCityAndNameIgnoreCase(city, tmVenue.name()).orElseGet(() -> {
            Double lat=tmVenue.location()!=null ? parse(tmVenue.location().latitude()) : null;
            Double lon=tmVenue.location()!=null ? parse(tmVenue.location().longitude()) : null;
            String street=tmVenue.address()!=null ? tmVenue.address().line1() : null;
            Address address=new Address(street, null, tmVenue.postalCode(), lat, lon);
            return venueRepository.save(new Venue(tmVenue.name(), null, address, VenueType.LIVE_MUSIC, city));
        });
    }

    private LocalDateTime toStart(TicketmasterResponse.TmEvent tmEvent) {
        if (tmEvent.dates()==null || tmEvent.dates().start()==null
                || tmEvent.dates().start().localDate()==null) return null;
        LocalDate date=LocalDate.parse(tmEvent.dates().start().localDate());
        String time=tmEvent.dates().start().localTime();
        return time!=null ? LocalDateTime.of(date, LocalTime.parse(time)) : date.atStartOfDay();
    }

    private VenueType toVenueType(String amenity) {
        if (amenity==null) return null;
        return switch (amenity) {
            case "bar" -> VenueType.BAR;
            case "pub" -> VenueType.PUB;
            case "nightclub" -> VenueType.CLUB;
            case "biergarten" -> VenueType.BEER_GARDEN;
            default -> null;
        };
    }

    private Double parse(String value) {
        try {
            return value==null ? null : Double.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
    private String toImageUrl(Map<String, String> tags) {
        String image=tags.get("image");
        if (image!=null && image.startsWith("http")) return image;

        String commons=tags.get("wikimedia_commons");
        if (commons==null && image!=null && image.startsWith("File:"))
            commons=image;
        if (commons!=null && commons.startsWith("File:")) {
            String file=commons.substring(5).replace(' ', '_');
            return "https://commons.wikimedia.org/wiki/Special:FilePath/"
                    + URLEncoder.encode(file, StandardCharsets.UTF_8) + "?width=300";
        }
        return null;
    }

    private String pickImage(List<TicketmasterResponse.TmImage> images) {
        if (images==null || images.isEmpty())
            return null;
        return images.stream()
                .filter(i->i.url()!=null)
                .min(Comparator.comparingInt((TicketmasterResponse.TmImage i) -> "16_9".equals(i.ratio()) ? 0 : 1)
                        .thenComparingInt(i -> Math.abs((i.width() != null ? i.width() : 0) - 640)))
                .map(TicketmasterResponse.TmImage::url)
                .orElse(null);
    }

    //ticketmaster uses "Undefined" when it has no genre
    private String cleanName(TicketmasterResponse.Named n) {
        if (n==null || n.name()==null || n.name().equalsIgnoreCase("Undefined"))
            return null;
        return n.name();
    }

    @Transactional
    public void refreshEvents(City city) {
        LocalDate today=Night.current();
        eventRepository.deleteForRefresh(city,
                today.minusDays(1).atStartOfDay(),
                today.atStartOfDay());
        importEvents(city);
        city.setEventsUpdatedOn(today);
        cityRepository.save(city);
    }
}