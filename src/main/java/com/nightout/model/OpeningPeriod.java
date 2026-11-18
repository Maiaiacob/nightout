package com.nightout.model;

import jakarta.persistence.*;

@Entity
@Table(name="venue_opening_period")
public class OpeningPeriod {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch= FetchType.LAZY, optional=false)
    @JoinColumn(name="venue_id")
    private Venue venue;

    @Column(nullable=false)
    private short dayOfWeek;

    @Column(nullable=false)
    private int opensMinute;

    @Column(nullable=false)
    private int closesMinute;

    protected OpeningPeriod() {}

    public OpeningPeriod(Venue venue, int dayOfWeek, int opensMinute, int closesMinute) {
        this.venue = venue;
        this.dayOfWeek = (short) dayOfWeek;
        this.opensMinute = opensMinute;
        this.closesMinute = closesMinute;
    }

    public Long getId() { return id; }
    public Venue getVenue() { return venue; }
    public int getDayOfWeek() { return dayOfWeek; }
    public int getOpensMinute() { return opensMinute; }
    public int getClosesMinute() { return closesMinute; }
}
