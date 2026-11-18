package com.nightout.model;

import jakarta.persistence.*;
@Entity
public class Venue {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private long id;

    private String description;

    @Column(nullable=false)
    private String name;

    @ManyToOne(fetch=FetchType.LAZY, optional=false)
    @JoinColumn(name="city_id")
    private City city;

    @Embedded
    private Address address;

    @Enumerated(EnumType.STRING)
    @Column(nullable=false)
    private VenueType type;

    @Column(unique=true)
    private Long osmId;

    private String openingHours;
    private String imageUrl;
    private String wikidata;

    private Boolean hoursParsed;

    protected Venue() {}

    public Venue(String name, String description, Address address, VenueType type, City city) {
        this.name = name;
        this.description = description;
        this.address = address;
        this.type = type;
        this.city = city;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Address getAddress() { return address; }
    public VenueType getType() { return type; }
    public City getCity() { return city; }
    public Long getOsmId() {
        return osmId;
    }
    public String getOpeningHours() {
        return openingHours;
    }
    public String getImageUrl() {return imageUrl;}
    public String getWikidata() { return wikidata; }
    public Boolean getHoursParsed() { return hoursParsed; }
    public void setHoursParsed(Boolean hoursParsed) { this.hoursParsed = hoursParsed; }

    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setAddress(Address address) { this.address = address; }
    public void setType(VenueType type) { this.type = type; }
    public void setCity(City city) { this.city = city; }
    public void setOsmId(Long osmId) {
        this.osmId = osmId;
    }
    public void setOpeningHours(String openingHours) { this.openingHours = openingHours; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setWikidata(String wikidata) {
        this.wikidata = wikidata;
    }
}
