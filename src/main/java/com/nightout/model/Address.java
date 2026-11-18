package com.nightout.model;

import jakarta.persistence.Embeddable;

@Embeddable
public class Address {
    private String street;
    private String houseNumber;
    private String postalCode;
    private Double latitude;
    private Double longitude;

    public Address(String street, String houseNumber, String postalCode,
                   Double latitude, Double longitude) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
        this.latitude = latitude;
        this.longitude = longitude;
    }
    protected Address() {}

    public Address(String street, String houseNumber, String postalCode) {
        this.street = street;
        this.houseNumber = houseNumber;
        this.postalCode = postalCode;
    }

    public String getStreet() {
        return street;
    }
    public String getHouseNumber() {
        return houseNumber;
    }
    public String getPostalCode() {
        return postalCode;
    }
    public Double getLatitude() {return latitude;}
    public Double getLongitude() {return longitude;}

    public void setStreet(String street) { this.street = street; }
    public void setHouseNumber(String houseNumber) { this.houseNumber = houseNumber; }
    public void setPostalCode(String postalCode) { this.postalCode = postalCode; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
