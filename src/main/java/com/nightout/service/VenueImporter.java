package com.nightout.service;

import com.nightout.client.OverpassClient;
import com.nightout.client.dto.OverpassResponse;
import com.nightout.model.City;
import com.nightout.repository.VenueRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Loads the bars of a city from Overpass in the background, so a search can answer right away
 * with the events while the (slow) bar import is still running.
 */
@Service
public class VenueImporter {

    private static final Logger log=LoggerFactory.getLogger(VenueImporter.class);
    private static final int SEARCH_RADIUS_METERS=4000; //city centre
    private static final Duration RETRY_AFTER_FAILURE=Duration.ofMinutes(10);

    private final OverpassClient overpassClient;
    private final ImportService importService;
    private final VenueRepo venueRepository;

    private final Set<Long> running=ConcurrentHashMap.newKeySet(); //cities that are being loaded right now
    private final Map<Long, Instant> failedAt=new ConcurrentHashMap<>(); //when Overpass last failed per city

    public VenueImporter(OverpassClient overpassClient, ImportService importService, VenueRepo venueRepository) {
        this.overpassClient = overpassClient;
        this.importService = importService;
        this.venueRepository = venueRepository;
    }

    public boolean isLoading(City city) {
        return running.contains(city.getId());
    }

    /** Loading the bars in the background. */
    public void startIfNeeded(City city) {
        if (venueRepository.existsByCityAndOsmIdIsNotNull(city)) return;
        Instant failed = failedAt.get(city.getId());
        if (failed != null && failed.plus(RETRY_AFTER_FAILURE).isAfter(Instant.now())) return;
        if (!running.add(city.getId())) return;

        Runnable job = () -> Thread.ofVirtual().start(() -> {
            try {
                load(city);
            } finally {
                running.remove(city.getId());
            }
        });

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { job.run(); }
                @Override public void afterCompletion(int status) {
                    if (status != STATUS_COMMITTED) running.remove(city.getId());
                }
            });
        } else {
            job.run();
        }
    }

    public void loadNow(City city) {
        if (venueRepository.existsByCityAndOsmIdIsNotNull(city) || !running.add(city.getId())) return;
        try {
            load(city);
        } finally {
            running.remove(city.getId());
        }
    }

    private void load(City city) {
        long started = System.currentTimeMillis();
        List<OverpassResponse.Element> places=
                overpassClient.findNightlife(city.getLatitude(), city.getLongitude(), SEARCH_RADIUS_METERS);
        if (places.isEmpty()) {
            failedAt.put(city.getId(), Instant.now());
            log.warn("No bars loaded for {}, will try again in {} minutes", city.getName(), RETRY_AFTER_FAILURE.toMinutes());
            return;
        }
        importService.saveVenues(city.getId(), places);
        failedAt.remove(city.getId());
        log.info("Loaded {} bars for {} in {} ms", places.size(), city.getName(), System.currentTimeMillis()-started);
    }
}
