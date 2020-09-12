package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.utils.AdvertisementCategory;
import com.ddkolesnik.siteparser.utils.AdvertisementType;
import com.ddkolesnik.siteparser.utils.City;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@Component
public class ScheduledTask {

    private final AdvertisementService advertisementService;

    private final AvitoParseService avitoParseService;

    public ScheduledTask(AdvertisementService advertisementService, AvitoParseService avitoParseService) {
        this.advertisementService = advertisementService;
        this.avitoParseService = avitoParseService;
    }

    /*
    Для ежедневного запуска:
    1. Получаем максимальную дату публикации из базы данных
    2. Проверяем первые 3 страницы объявлений
    3. Собираем новые объявления
     */
    @Scheduled(cron = "${cron.expression.daily}")
    public void runDaily() {
        LocalDate maxPublishDate = advertisementService.getMaxPublishDate();
        log.info("Начинаем ЕЖЕДНЕВНЫЙ сбор объявлений старше {}", maxPublishDate);
        int count = parse(maxPublishDate);
        log.info("Завершено, собрано объявлений [{} шт]", count);
    }

    @Scheduled(cron = "${cron.expression.weekly}")
    public void runWeekly() {
        LocalDateTime now = LocalDateTime.now();
        log.info("Начинаем ЕЖЕНЕДЕЛЬНЫЙ сбор объявлений");
        int count = parse(null);
        log.info("Завершено, собрано объявлений [{} шт]", count);
        log.info("Удаляем данные старше {}", now);
        advertisementService.deleteOld(now);
        log.info("Завершено");
    }

    private int parse(LocalDate maxPublishDate) {
        int count = avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.SALE, City.MOSCOW, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.RENT, City.MOSCOW, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.SALE, City.MOSCOW, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.RENT, City.MOSCOW, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.SALE, City.TYUMEN, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.RENT, City.TYUMEN, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.SALE, City.TYUMEN, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.RENT, City.TYUMEN, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.SALE, City.EKB, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.TRADING_AREA, AdvertisementType.RENT, City.EKB, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.SALE, City.EKB, maxPublishDate);
        count += avitoParseService.parse(AdvertisementCategory.OTHER, AdvertisementType.RENT, City.EKB, maxPublishDate);
        return count;
    }

}
