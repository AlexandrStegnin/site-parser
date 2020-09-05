package com.ddkolesnik.siteparser.controller;

import com.ddkolesnik.siteparser.service.AdvertisementService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@RestController
public class MainController {

    private final AdvertisementService advertisementService;

    public MainController(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    @GetMapping(path = "/count")
    public String count() {
        return String.format("Всего объявлений в базе данных [%d шт]", advertisementService.count());
    }

}
