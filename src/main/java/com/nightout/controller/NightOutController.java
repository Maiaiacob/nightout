package com.nightout.controller;


import com.nightout.dto.NightOutDto;
import com.nightout.service.NightOutService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class NightOutController {

    private final NightOutService nightOutService;

    public NightOutController(NightOutService nightOutService) {
        this.nightOutService = nightOutService;
    }

    @GetMapping("/api/nightout")
    public NightOutDto search(@RequestParam String city) {
        return nightOutService.search(city);
    }
}