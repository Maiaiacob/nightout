package com.nightout.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;

public record OverpassResponse(List<Element> elements) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Element(long id, double lat, double lon, Map<String, String> tags) {}
}
