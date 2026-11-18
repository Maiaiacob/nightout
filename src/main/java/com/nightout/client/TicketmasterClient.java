package com.nightout.client;

import com.nightout.client.dto.TicketmasterResponse;
import com.nightout.config.NightoutProperties;
import com.nightout.util.Night;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
public class TicketmasterClient {
    private static final Logger log = LoggerFactory.getLogger(TicketmasterClient.class);
    private static final DateTimeFormatter TM_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final RestClient restClient;
    private final String apiKey;

    public TicketmasterClient(NightoutProperties props) {
        this.apiKey = props.ticketmaster().apiKey();
        this.restClient = RestClient.builder().baseUrl(props.ticketmaster().baseUrl()).build();
    }

    /** 1 day means today 00:00 until tomorrow 06:00 */
    public List<TicketmasterResponse.TmEvent> findEvents(double lat, double lon, int days) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("No Ticketmaster API key set, skipping events");
            return List.of();
        }

        ZonedDateTime from = Night.current().atStartOfDay(ZoneId.systemDefault());
        ZonedDateTime to = from.plusDays(days).plusHours(6);
        String start = from.withZoneSameInstant(ZoneOffset.UTC).format(TM_FORMAT);
        String end = to.withZoneSameInstant(ZoneOffset.UTC).format(TM_FORMAT);

        try {
            TicketmasterResponse response = restClient.get()
                    .uri(uri -> uri.path("/events.json")
                            .queryParam("apikey", apiKey)
                            .queryParam("latlong", lat + "," + lon)
                            .queryParam("radius", 15)
                            .queryParam("unit", "km")
                            .queryParam("startDateTime", start)
                            .queryParam("endDateTime", end)
                            .queryParam("sort", "date,asc")
                            .queryParam("size", 200)
                            .build())
                    .retrieve()
                    .body(TicketmasterResponse.class);

            if (response == null || response.embedded() == null || response.embedded().events() == null) {
                return List.of();
            }
            return response.embedded().events();
        } catch (RestClientException e) {
            log.error("Ticketmaster request failed: {}", e.getMessage());
            return List.of();
        }
    }
}