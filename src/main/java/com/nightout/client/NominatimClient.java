package com.nightout.client;

import com.nightout.client.dto.NominatimPlace;
import com.nightout.config.NightoutProperties;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Optional;

@Component
public class NominatimClient {
    private final RestClient restClient;

    public NominatimClient(NightoutProperties props) {
        this.restClient = RestClient.builder()
                .baseUrl(props.nominatim().baseUrl())
                .defaultHeader(HttpHeaders.USER_AGENT, props.nominatim().userAgent())
                .build();
    }

    public Optional<NominatimPlace> findCity(String city) {
        NominatimPlace[] results = restClient.get()
                .uri(uri -> uri.path("/search")
                        .queryParam("q", city)
                        .queryParam("format", "jsonv2")
                        .queryParam("addressdetails", 1)
                        .queryParam("limit", 1)
                        .build())
                .retrieve()
                .body(NominatimPlace[].class);

        return (results == null || results.length == 0) ? Optional.empty() : Optional.of(results[0]);
    }
}
