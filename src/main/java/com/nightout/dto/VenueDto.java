package com.nightout.dto;

public record VenueDto(Long id,
                       String name,
                       String description,
                       String type,
                       String street,
                       String houseNumber,
                       String postalCode,
                       String cityName,
                       Double latitude,
                       Double longitude,
                       String openingHours,
                      String imageUrl,
                      String wikidata,
                      Boolean openNow,
                      String openUntil) {
}
