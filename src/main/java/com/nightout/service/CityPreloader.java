package com.nightout.service;

import com.nightout.model.City;
import com.nightout.repository.CityRepo;
import com.nightout.repository.VenueRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConditionalOnProperty(name = "nightout.preload.enabled", havingValue = "true", matchIfMissing = true)
public class CityPreloader implements ApplicationRunner {

    private static final Logger log=LoggerFactory.getLogger(CityPreloader.class);

    private static final List<String> CITIES=List.of(
            "Amsterdam", "Berlin", "Timisoara", "Bucharest", "Cluj-Napoca",
            "Vienna", "Prague", "Budapest", "Barcelona", "Lisbon"
    );

    private final CityRepo cityRepository;
    private final ImportService importService;
    private final VenueImporter venueImporter;
    private final VenueRepo venueRepository;

    public CityPreloader(CityRepo cityRepository, ImportService importService,
                         VenueImporter venueImporter, VenueRepo venueRepository) {
        this.cityRepository = cityRepository;
        this.importService = importService;
        this.venueImporter = venueImporter;
        this.venueRepository = venueRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        Thread.ofVirtual().start(() -> {
            for (String name : CITIES) {
                boolean alreadyThere = cityRepository.findByNameIgnoreCase(name)
                        .filter(c -> c.getLatitude() != null && venueRepository.existsByCityAndOsmIdIsNotNull(c))
                        .isPresent();
                if (alreadyThere) continue;

                try {
                    log.info("Preloading {}", name);
                    City city = importService.importCity(name);
                    venueImporter.loadNow(city);
                    Thread.sleep(5000);
                } catch (Exception e) {
                    log.warn("Preloading {} failed: {}", name, e.getMessage());
                }
            }
            log.info("Preloading done");
        });
    }
}