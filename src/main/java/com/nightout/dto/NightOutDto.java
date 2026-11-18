package com.nightout.dto;

import java.util.List;

// venuesLoading = true while the bars are still being fetched in the background
public record NightOutDto(CityDto city, List<EventDto> events, List<VenueDto> venues, boolean venuesLoading) {}

