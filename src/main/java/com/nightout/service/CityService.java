package com.nightout.service;

import com.nightout.dto.CityDto;
import com.nightout.mapper.CityMapper;
import com.nightout.repository.CityRepo;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly=true)
public class CityService {
    private final CityRepo cityRepository;
    private final CityMapper cityMapper;

    public CityService(CityRepo cityRepository, CityMapper cityMapper) {
        this.cityRepository = cityRepository;
        this.cityMapper = cityMapper;
    }

    public List<CityDto> findAll() {
        return cityRepository.findAll(Sort.by("name")).stream()
                .map(cityMapper::toDto)
                .toList();
    }
}
