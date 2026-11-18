package com.nightout.model;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
public class Event {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    private String description;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false)
    private LocalDateTime startTime;

    private LocalDateTime endTime;

    private BigDecimal entryPrice;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="venue_id")
    private Venue venue;

    @Column(unique=true)
    private String externalId;

    private String url;

    private String imageUrl;
    private String category;
    private String genre;

    protected Event() {}

    public Event(String name, String description, LocalDateTime startTime,
                 LocalDateTime endTime, BigDecimal entryPrice, Venue venue) {
        this.name = name;
        this.description = description;
        this.startTime = startTime;
        this.endTime = endTime;
        this.entryPrice = entryPrice;
        this.venue = venue;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LocalDateTime getStartTime() { return startTime; }
    public LocalDateTime getEndTime() { return endTime; }
    public BigDecimal getEntryPrice() { return entryPrice; }
    public Venue getVenue() { return venue; }
    public String getExternalId() {
        return externalId;
    }
    public String getUrl() {
        return url;
    }
    public String getCategory() { return category; }
    public String getGenre() { return genre; }
    public String getImageUrl() { return imageUrl; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }
    public void setEntryPrice(BigDecimal entryPrice) { this.entryPrice = entryPrice; }
    public void setVenue(Venue venue) { this.venue = venue; }
    public void setExternalId(String externalId) {
        this.externalId = externalId;
    }
    public void setUrl(String url) {
        this.url = url;
    }
    public void setCategory(String category) { this.category = category; }
    public void setGenre(String genre) { this.genre = genre; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
}
