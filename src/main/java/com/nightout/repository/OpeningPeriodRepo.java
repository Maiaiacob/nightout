package com.nightout.repository;

import com.nightout.model.OpeningPeriod;
import com.nightout.model.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface OpeningPeriodRepo extends JpaRepository<OpeningPeriod, Long> {

    List<OpeningPeriod> findByVenueOrderByDayOfWeekAscOpensMinuteAsc(Venue venue);

    @Modifying(flushAutomatically=true)
    @Query("delete from OpeningPeriod p where p.venue = :venue")
    void deleteByVenue(@Param("venue") Venue venue);

    @Query(value = """
            SELECT p.venue_id, p.closes_minute FROM venue_opening_period p
            WHERE p.venue_id IN (:ids)
              AND (   (p.day_of_week = :day AND p.opens_minute <= :minute AND p.closes_minute > :minute)
                   OR (p.day_of_week = :previousDay AND p.closes_minute > 1440 + :minute))
            """, nativeQuery = true)
    List<Object[]> findOpenSlots(@Param("ids") Collection<Long> ids, @Param("day") int day,
                                 @Param("previousDay") int previousDay, @Param("minute") int minute);
}
