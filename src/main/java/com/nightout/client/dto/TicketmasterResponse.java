package com.nightout.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
@JsonIgnoreProperties(ignoreUnknown = true)
public record TicketmasterResponse(@JsonProperty("_embedded") Embedded embedded) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Embedded(List<TmEvent> events) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmEvent(String id, String name, String url, Dates dates,
                          List<TmImage> images,
                          List<Classification> classifications,
                          @JsonProperty("_embedded") EventEmbedded embedded) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmImage(String url, String ratio, Integer width) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Classification(Named segment, Named genre) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Named(String name) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Dates(Start start) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Start(String localDate, String localTime) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EventEmbedded(List<TmVenue> venues) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmVenue(String name, String postalCode, TmAddress address, TmLocation location) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmAddress(String line1) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TmLocation(String latitude, String longitude) {}
}
