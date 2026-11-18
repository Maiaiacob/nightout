package com.nightout.repository;

import com.nightout.model.City;
import com.nightout.model.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

public interface EventRepo extends JpaRepository<Event, Long> {
    List<Event> findByVenueId(Long venueId);
    List<Event> findByVenueCityNameIgnoreCaseAndStartTimeBetweenOrderByStartTime(
            String cityName, LocalDateTime from, LocalDateTime to);
    boolean existsByExternalId(String externalId);

    @Modifying(flushAutomatically=true)
    @Query("""
       delete from Event e
       where e.venue.id in (select v.id from Venue v where v.city = :city)
         and (e.startTime < :keepFrom or e.startTime >= :refetchFrom)
       """)
    int deleteForRefresh(@Param("city") City city,
                         @Param("keepFrom") LocalDateTime keepFrom,
                         @Param("refetchFrom") LocalDateTime refetchFrom);

    @Modifying
    @Transactional
    @Query("delete from Event e where e.startTime < :before")
    int deleteOlderThan(@Param("before") LocalDateTime before);
}
