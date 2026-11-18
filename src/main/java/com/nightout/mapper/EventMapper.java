package com.nightout.mapper;

import com.nightout.dto.EventDto;
import com.nightout.model.Event;
import com.nightout.model.Venue;
import org.springframework.stereotype.Component;

@Component
public class EventMapper {
    public EventDto toDto(Event event) {
        Venue venue = event.getVenue();
        return new EventDto(
                event.getId(),
                event.getName(),
                event.getDescription(),
                event.getStartTime(),
                event.getEndTime(),
                event.getEntryPrice(),
                venue.getId(),
                venue.getName(),
                event.getUrl(),
                event.getImageUrl(),
                event.getCategory(),
                event.getGenre()
        );
    }
}
