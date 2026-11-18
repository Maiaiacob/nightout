package com.nightout.dto;

public record CityDto(Long id,
                      String name,
                      String country,
                      Double latitude,
                      Double longitude) {
}
