package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.utils.AdvertisementCategory;
import com.ddkolesnik.siteparser.utils.AdvertisementType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

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

    @Scheduled(cron = "0 0 0 ? * *")
    public void run() {
        log.info("Начинаем сбор объявлений");
        log.info("Всего собрано объявлений [{} шт]", parse());
    }

    private int parse() {
        advertisementService.deleteAll();
        int list1 = avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.SALE, 1);
        log.info("Объявления о ПРОДАЖЕ торговых площадей собраны [{} шт]", list1);
        int list2 = avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.RENT, 1);
        log.info("Объявления об АРЕНДЕ торговых площадей собраны [{} шт]", list2);
        int list3 = avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.SALE, 1);
        log.info("Объявления о ПРОДАЖЕ других категорий объектов собраны [{} шт]", list3);
        int list4 = avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.RENT, 1);
        log.info("Объявления об АРЕНДЕ других категорий объектов собраны [{} шт]", list4);
        return list1 + list2 + list3 + list4;
    }

}
