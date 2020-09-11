package com.ddkolesnik.siteparser.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@Component
public class ScheduledTask {

    private final Parser parser;

    private final AdvertisementService advertisementService;

    public ScheduledTask(Parser parser, AdvertisementService advertisementService) {
        this.parser = parser;
        this.advertisementService = advertisementService;
    }

    @Scheduled(cron = "${cron.expression}")
    public void run() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Начинаем сбор объявлений");
        int rentCount = parser.parseRent();
        int saleCount = parser.parseSale();
        log.info("Всего собрано объявлений [{} шт]", saleCount + rentCount);
        log.info("Удаляем старые данные");
        advertisementService.deleteOld(now);
        log.info("Готово");
    }

}
