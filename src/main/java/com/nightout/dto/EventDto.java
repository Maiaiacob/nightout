package com.nightout.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record EventDto(Long id,
                       String name,
                       String description,
                       LocalDateTime startTime,
                       LocalDateTime endTime,
                       BigDecimal entryPrice,
                       Long venueId,
                       String venueName,
                       String url,
                       String imageUrl,
                       String category,
                       String genre) {
}
