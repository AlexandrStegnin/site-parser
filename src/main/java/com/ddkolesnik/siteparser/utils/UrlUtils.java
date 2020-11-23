package com.ddkolesnik.siteparser.utils;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;

/**
 * @author Alexandr Stegnin
 */

public class UrlUtils {

    private final static String TEMPLATE = "https://avito.ru/%s/%s/%s&s=104";

    private static final String PRICE_PART = "&pmin=";

    private UrlUtils() {
    }

    /**
     * Ссылка на продажу торговых площадей
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getTradingAreaSaleUrl(City city) {
        String price = PRICE_PART.concat(city.getSalePrice());
        String part = "prodam/magazin-ASgBAQICAUSwCNJWAUCGCRSQXQ" +
                "&proprofile=1&f=ASgBAQICAkSwCNJW8hKg2gEBQIYJFJBd&i=1" + price;
        return generateUrl(city.getTitle(), AdvCategory.COMMERCIAL_PROPERTY.getCategory(), part);
    }

    /**
     * Ссылка на аренду торговых площадей
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getTradingAreaRentUrl(City city) {
        String price = PRICE_PART.concat(city.getRentPrice());
        String part = "sdam/magazin-ASgBAQICAUSwCNRWAUDUCBS8WQ?cd=1&f=ASgBAQICAkSwCNRW9BKk2gEBQNQIFLxZ" +
                price + "&proprofile=1";
        return generateUrl(city.getTitle(), AdvCategory.COMMERCIAL_PROPERTY.getCategory(), part);
    }

    /**
     * Ссылка на продажу остальных категорий объектов
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getOtherCategoriesSaleUrl(City city) {
        String price = PRICE_PART.concat(city.getSalePrice());
        String part = "prodam-ASgBAgICAUSwCNJW?" +
                "f=ASgBAQICAkSwCNJW8hKg2gEBQIYJRIqsAcD_AY5dil0" + price +
                "&proprofile=1&";
        return generateUrl(city.getTitle(), AdvCategory.COMMERCIAL_PROPERTY.getCategory(), part);
    }

    /**
     * Ссылка на аренду остальных категорий объектов
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getOtherCategoriesRentUrl(City city) {
        String price = PRICE_PART.concat(city.getSalePrice());
        String part = "sdam-ASgBAgICAUSwCNRW?f=ASgBAQICAkSwCNRW9BKk2gEBQNQIRIysAb7_AbpZtlk" + price +
                "&proprofile=1";
        return generateUrl(city.getTitle(), AdvCategory.COMMERCIAL_PROPERTY.getCategory(), part);
    }

    /**
     * Ссылка на покупку в категории дома, дачи, коттеджи
     *
     * @param city город
     * @return ссылка
     */
    public static String getHouseCountryHouseCottageSaleUrl(City city) {
        String part = "prodam-ASgBAgICAUSUA9AQ?cd=1&i=1";
        return generateUrl(city.getTitle(), AdvCategory.HOUSE_COUNTRY_HOUSE_COTTAGE.getCategory(), part);
    }

    /**
     * Ссылка на аренду в категории дома, дачи, коттеджи
     *
     * @param city город
     * @return ссылка
     */
    public static String getHouseCountryHouseCottageRentUrl(City city) {
        String part = "sdam-ASgBAgICAUSUA9IQ?cd=1&i=1";
        return generateUrl(city.getTitle(), AdvCategory.HOUSE_COUNTRY_HOUSE_COTTAGE.getCategory(), part);
    }

    /**
     * Ссылка на покупку/аренду в категории земельные участки
     *
     * @param city город
     * @param type тип объявления
     * @return ссылка
     */
    public static String getSteadUrl(City city, AdvertisementType type) {
        String part = "";
        switch (type) {
            case RENT:
                part = "sdam-ASgBAgICAUSWA9wQ?i=1";
                break;
            case SALE:
                part = "prodam-ASgBAgICAUSWA9oQ?i=1";
                break;
        }
        return generateUrl(city.getTitle(), AdvCategory.STEAD.getCategory(), part);
    }

    /**
     * Сгенерировать ссылку по шаблону
     *
     * @param city название города
     * @param category категория
     * @param part часть ссылки
     * @return ссылка
     */
    private static String generateUrl(String city, String category, String part) {
        return String.format(TEMPLATE, city, category, part);
    }


    /**
     * Подключение SSL
     *
     * @throws KeyManagementException   при отсутствии KeyManager
     * @throws NoSuchAlgorithmException при отсутствии алгоритма
     */
    public static void enableSSLSocket() throws KeyManagementException, NoSuchAlgorithmException {
        HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, new X509TrustManager[]{new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] chain, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] chain, String authType) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return new X509Certificate[0];
            }
        }}, new SecureRandom());
        HttpsURLConnection.setDefaultSSLSocketFactory(context.getSocketFactory());
    }

}
