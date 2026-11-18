package com.nightout.repository;

import com.nightout.model.City;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CityRepo extends JpaRepository<City, Long> {
    Optional<City> findByNameIgnoreCase(String name);
}
