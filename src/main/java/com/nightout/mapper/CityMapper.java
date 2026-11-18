package com.nightout.mapper;

import com.nightout.dto.CityDto;
import com.nightout.model.City;
import org.springframework.stereotype.Component;

@Component
public class CityMapper {

    public CityDto toDto(City city) {
        return new CityDto(
                city.getId(),
                city.getName(),
                city.getCountry(),
                city.getLatitude(),
                city.getLongitude()
        );
    }
}
