package com.nightout.client;

import com.nightout.client.dto.OverpassResponse;
import com.nightout.config.NightoutProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Component
public class OverpassClient {

    private static final Logger log = LoggerFactory.getLogger(OverpassClient.class);

    private final RestClient restClient;
    private final List<String> servers;
    private final JsonMapper jsonMapper;
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
    private static final long HEDGE_DELAY_SECONDS = 15;

    public OverpassClient(NightoutProperties props, JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(90));

        this.restClient = RestClient.builder()
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.USER_AGENT, props.nominatim().userAgent())
                .build();

        this.servers = java.util.stream.Stream.of(
                props.overpass().baseUrl(),
                "https://overpass.private.coffee/api",
                "https://overpass-api.de/api",
                "https://maps.mail.ru/osm/tools/overpass/api"
        ).distinct().toList();
    }

    /** Bars, pubs, clubs and beer gardens within radiusMeters of the city centre */
    public List<OverpassResponse.Element> findNightlife(double lat, double lon, int radiusMeters) {
        String query = String.format(Locale.ROOT, """
                [out:json][timeout:60];
                node["amenity"~"^(bar|pub|nightclub|biergarten)$"]["name"](around:%d,%f,%f);
                out body 300;
                """, radiusMeters, lat, lon);
        String body = "data=" + URLEncoder.encode(query, StandardCharsets.UTF_8);

        CompletableFuture<List<OverpassResponse.Element>> winner = new CompletableFuture<>();
        List<CompletableFuture<List<OverpassResponse.Element>>> attempts = new ArrayList<>();
        for (int i = 0; i < servers.size(); i++) {
            String server = servers.get(i);
            var delayed = CompletableFuture.delayedExecutor(i * HEDGE_DELAY_SECONDS, TimeUnit.SECONDS, executor);
            var attempt = CompletableFuture.supplyAsync(() -> {
                if (winner.isDone()) return List.<OverpassResponse.Element>of();
                List<OverpassResponse.Element> places = ask(server, body);
                if (!places.isEmpty()) winner.complete(places);
                return places;
            }, delayed);
            attempts.add(attempt);
        }

        CompletableFuture.allOf(attempts.toArray(CompletableFuture[]::new))
                .thenRun(() -> {
                    if (winner.complete(List.of())) log.error("All Overpass servers failed");
                });
        return winner.join();
    }

    private List<OverpassResponse.Element> ask(String server, String body) {
        try {
            byte[] bytes = restClient.post()
                    .uri(server + "/interpreter")
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(byte[].class);
            String text = bytes == null ? null : new String(bytes, StandardCharsets.UTF_8);


            if (text == null || !text.stripLeading().startsWith("{")) {
                String start = text == null ? "nothing" : text.substring(0, Math.min(120, text.length()));
                log.warn("Overpass server {} sent no JSON: {}", server, start.replaceAll("\\s+", " "));
                return List.of();
            }
            OverpassResponse response = jsonMapper.readValue(text, OverpassResponse.class);


            if (response != null && response.elements() != null && !response.elements().isEmpty()) {
                log.info("Overpass: {} places from {}", response.elements().size(), server);
                return response.elements();
            }
            log.warn("Overpass server {} returned no places", server);
        } catch (RestClientException | JacksonException e) {
            log.warn("Overpass server {} failed: {}", server, e.getMessage());
        }
        return List.of();
    }
}