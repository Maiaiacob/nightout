package com.nightout.model;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
public class City {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    @Column(nullable=false)
    private String name;

    @Column(nullable=false)
    private String country;

    private Double latitude;
    private Double longitude;

    private LocalDate eventsUpdatedOn;

    protected City() {}

    public City(String name, String country) {
        this.name=name;
        this.country=country;
    }
    public long getId() {return id;}
    public String getName() {return name;}
    public String getCountry() {return country;}
    public Double getLatitude() {return latitude;}
    public Double getLongitude() {return longitude;}
    public LocalDate getEventsUpdatedOn() {return eventsUpdatedOn;}

    public void setName(String name) { this.name = name; }
    public void setCountry(String country) { this.country = country; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
    public void setEventsUpdatedOn(LocalDate eventsUpdatedOn) {
        this.eventsUpdatedOn = eventsUpdatedOn;
    }
}
