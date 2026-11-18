package com.nightout.dto;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

/** A venue plus how far it is from the user. In JSON the venue fields are flattened, with distanceMeters next to them. */
public record NearbyVenueDto(@JsonUnwrapped VenueDto venue, int distanceMeters) {
}
