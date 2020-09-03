package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public class UrlUtils {

    private UrlUtils() {
    }

    /**
     * Ссылка на продажу торговых площадей
     *
     * @param pageNumber номер страницы
     * @return ссылка на страницу
     */
    public static String getTradingAreaSaleUrl(int pageNumber) {
        return "https://www.avito.ru/moskva/kommercheskaya_nedvizhimost/prodam/" +
                "magazin-ASgBAQICAUSwCNJWAUCGCRSQXQ?pmin=17900000&" +
                "proprofile=1&f=ASgBAQICAkSwCNJW8hKg2gEBQIYJFJBd&i=1&p=" + pageNumber;
    }

    /**
     * Ссылка на аренду торговых площадей
     *
     * @param pageNumber номер страницы
     * @return ссылка на страницу
     */
    public static String getTradingAreaRentUrl(int pageNumber) {
        return "https://www.avito.ru/moskva/kommercheskaya_nedvizhimost/sdam/" +
                "magazin-ASgBAQICAUSwCNRWAUDUCBS8WQ?cd=1&f=ASgBAQICAkSwCNRW9BKk2gEBQNQIFLxZ" +
                "&pmin=17900000&proprofile=1&p=" + pageNumber;
    }

    /**
     * Ссылка на продажу остальных категорий объектов
     *
     * @param pageNumber номер страницы
     * @return ссылка на страницу
     */
    public static String getOtherCategoriesSaleUrl(int pageNumber) {
        return "https://www.avito.ru/moskva/kommercheskaya_nedvizhimost/prodam-ASgBAgICAUSwCNJW?cd=1" +
                "&f=ASgBAQICAkSwCNJW8hKg2gEBQIYJRIqsAcD_AY5dil0" +
                "&pmin=17900000&proprofile=1&p=" + pageNumber;
    }

    /**
     * Ссылка на аренду остальных категорий объектов
     *
     * @param pageNumber номер страницы
     * @return ссылка на страницу
     */
    public static String getOtherCategoriesRentUrl(int pageNumber) {
        return "https://www.avito.ru/moskva/kommercheskaya_nedvizhimost/" +
                "sdam-ASgBAgICAUSwCNRW?cd=1&f=ASgBAQICAkSwCNRW9BKk2gEBQNQIRIysAb7_AbpZtlk" +
                "&pmin=17900000&proprofile=1&p=" + pageNumber;
    }

}
