package com.nightout.model;

public enum VenueType {
    BAR("Bar"),
    PUB("Pub"),
    CLUB("Club"),
    LOUNGE("Lounge"),
    COCKTAIL_BAR("Cocktail bar"),
    BEER_GARDEN("Beer garden"),
    LIVE_MUSIC("Live music venue"),
    KARAOKE("Karaoke bar");

    private final String displayName;

    VenueType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
