package com.nightout.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nightout")
public record NightoutProperties(Ticketmaster ticketmaster, Nominatim nominatim, Overpass overpass) {
    public record Ticketmaster(String baseUrl, String apiKey) {}
    public record Nominatim(String baseUrl, String userAgent) {}
    public record Overpass(String baseUrl) {}
}
