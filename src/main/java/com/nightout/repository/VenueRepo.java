package com.nightout.repository;

import com.nightout.model.City;
import com.nightout.model.Venue;
import com.nightout.model.VenueType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VenueRepo extends JpaRepository<Venue, Long> {

    List<Venue> findByCityNameIgnoreCase(String cityName);
    List<Venue> findByCityNameIgnoreCaseAndType(String cityName, VenueType type);
    boolean existsByOsmId(Long osmId);
    boolean existsByCityAndOsmIdIsNotNull(City city);
    Optional<Venue> findFirstByCityAndNameIgnoreCase(City city, String name);

    /**Venues within "radiusMeters" of a point(nearest first) */
    @Query(value = """
            SELECT v.* FROM venue v
            WHERE ST_DWithin(v.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)
            ORDER BY v.location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            LIMIT :limit
            """, nativeQuery = true)
    List<Venue> findNearby(@Param("lat") double lat, @Param("lng") double lng,
                           @Param("radius") double radiusMeters, @Param("limit") int limit);

    /**Venues who have opening hours that are not converted into time slots yet */
    List<Venue> findByHoursParsedIsNull();

    /**
     * Venues that are open at the given moment: a slot from today that has started and not ended
     * or a slot that has started yesterday and is running past midnight.
     */
    @Query(value = """
            SELECT v.* FROM venue v
            WHERE ST_DWithin(v.location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)
              AND EXISTS (
                  SELECT 1 FROM venue_opening_period p
                  WHERE p.venue_id = v.id
                    AND (   (p.day_of_week = :day AND p.opens_minute <= :minute AND p.closes_minute > :minute)
                         OR (p.day_of_week = :previousDay AND p.closes_minute > 1440 + :minute)))
            ORDER BY v.location <-> ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography
            LIMIT :limit
            """, nativeQuery = true)
    List<Venue> findNearbyOpenAt(@Param("lat") double lat, @Param("lng") double lng,
                                 @Param("radius") double radiusMeters, @Param("limit") int limit,
                                 @Param("day") int day, @Param("previousDay") int previousDay,
                                 @Param("minute") int minute);
}
