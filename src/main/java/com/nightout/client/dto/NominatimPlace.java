package com.nightout.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;
import java.util.Map;
@JsonIgnoreProperties(ignoreUnknown = true)
public record NominatimPlace(String name, String lat, String lon,
                             List<String> boundingbox,
                             Map<String, String> address) {
}
