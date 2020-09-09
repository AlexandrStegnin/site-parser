package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.utils.AdvertisementCategory;
import com.ddkolesnik.siteparser.utils.AdvertisementType;
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

    private final AvitoParseService avitoParseService;

    private final AdvertisementService advertisementService;

    public ScheduledTask(AvitoParseService avitoParseService, AdvertisementService advertisementService) {
        this.avitoParseService = avitoParseService;
        this.advertisementService = advertisementService;
    }

    @Scheduled(cron = "${cron.expression}")
    public void run() {
        log.info("Начинаем сбор объявлений");
        log.info("Всего собрано объявлений [{} шт]", parse());
    }

    private int parse() {
        int list4 = avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.RENT, 1);
        log.info("Объявления об АРЕНДЕ других категорий объектов собраны [{} шт]", list4);
        try {
            Thread.sleep(60_000);
        } catch (InterruptedException e) {
            log.error("Произошла ошибка: {}", e.getLocalizedMessage());
        }
        int list2 = avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.RENT, 1);
        log.info("Объявления об АРЕНДЕ торговых площадей собраны [{} шт]", list2);
        try {
            Thread.sleep(60_000);
        } catch (InterruptedException e) {
            log.error("Произошла ошибка: {}", e.getLocalizedMessage());
        }
        int list1 = avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.SALE, 1);
        log.info("Объявления о ПРОДАЖЕ торговых площадей собраны [{} шт]", list1);
        try {
            Thread.sleep(60_000);
        } catch (InterruptedException e) {
            log.error("Произошла ошибка: {}", e.getLocalizedMessage());
        }
        int list3 = avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.SALE, 1);
        log.info("Объявления о ПРОДАЖЕ других категорий объектов собраны [{} шт]", list3);
        advertisementService.deleteOld(LocalDateTime.now());
        return list1 + list2 + list3 + list4;
    }

}
