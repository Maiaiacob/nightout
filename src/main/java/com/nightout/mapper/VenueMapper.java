package com.nightout.mapper;

import com.nightout.dto.VenueDto;
import com.nightout.hours.OpeningHoursService.OpenNow;
import com.nightout.model.Address;
import com.nightout.model.Venue;
import org.springframework.stereotype.Component;

@Component
public class VenueMapper {
    public VenueDto toDto(Venue venue) {
        return toDto(venue, null);
    }

    public VenueDto toDto(Venue venue, OpenNow openNow) {
        Address a = venue.getAddress();
        return new VenueDto(
                venue.getId(),
                venue.getName(),
                venue.getDescription(),
                venue.getType().getDisplayName(),
                a != null ? a.getStreet() : null,
                a != null ? a.getHouseNumber() : null,
                a != null ? a.getPostalCode() : null,
                venue.getCity().getName(),
                a != null ? a.getLatitude() : null,
                a != null ? a.getLongitude() : null,
                venue.getOpeningHours(),
                venue.getImageUrl(),
                venue.getWikidata(),
                openNow != null ? openNow.open() : null,
                openNow != null ? openNow.until() : null
        );
    }
}
