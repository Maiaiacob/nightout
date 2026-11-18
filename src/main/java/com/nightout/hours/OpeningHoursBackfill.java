package com.nightout.hours;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/** At startup: convert the opening hours of venues imported before the time slots existed. */
@Component
@ConditionalOnProperty(name = "nightout.preload.enabled", havingValue = "true", matchIfMissing = true)
public class OpeningHoursBackfill implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(OpeningHoursBackfill.class);
    private final OpeningHoursService service;

    public OpeningHoursBackfill(OpeningHoursService service) {
        this.service = service;
    }

    @Override
    public void run(ApplicationArguments args) {
        OpeningHoursService.BackfillResult r = service.backfill();
        if (r.venues() > 0) {
            log.info("Opening hours: checked {} venues, {} have hours, {} understood ({}%)",
                    r.venues(), r.withHoursText(), r.parsed(),
                    r.withHoursText() == 0 ? 0 : Math.round(100.0 * r.parsed() / r.withHoursText()));
        }
    }
}
