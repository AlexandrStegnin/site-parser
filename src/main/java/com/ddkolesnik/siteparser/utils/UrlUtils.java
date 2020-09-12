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

    private UrlUtils() {
    }

    /**
     * Ссылка на продажу торговых площадей
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getTradingAreaSaleUrl(City city) {
        String baseUrl = "https://www.avito.ru/";
        String partUrl = "/kommercheskaya_nedvizhimost/prodam/magazin-ASgBAQICAUSwCNJWAUCGCRSQXQ?pmin=17900000&" +
                "proprofile=1&f=ASgBAQICAkSwCNJW8hKg2gEBQIYJFJBd&i=1&s=104";
        return baseUrl + city.getTitle() + partUrl;
    }

    /**
     * Ссылка на аренду торговых площадей
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getTradingAreaRentUrl(City city) {
        String baseUrl = "https://www.avito.ru/";
        String partUrl = "/kommercheskaya_nedvizhimost/sdam/" +
                "magazin-ASgBAQICAUSwCNRWAUDUCBS8WQ?cd=1&f=ASgBAQICAkSwCNRW9BKk2gEBQNQIFLxZ" +
                "&pmin=17900000&proprofile=1&s=104";
        return baseUrl + city.getTitle() + partUrl;
    }

    /**
     * Ссылка на продажу остальных категорий объектов
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getOtherCategoriesSaleUrl(City city) {
        String baseUrl = "https://www.avito.ru/";
        String partUrl = "/kommercheskaya_nedvizhimost/prodam-ASgBAgICAUSwCNJW?cd=1" +
                "&f=ASgBAQICAkSwCNJW8hKg2gEBQIYJRIqsAcD_AY5dil0" +
                "&pmin=17900000&proprofile=1&s=104";
        return baseUrl + city.getTitle() + partUrl;
    }

    /**
     * Ссылка на аренду остальных категорий объектов
     *
     * @param city город объявления
     * @return ссылка на страницу
     */
    public static String getOtherCategoriesRentUrl(City city) {
        String baseUrl = "https://www.avito.ru/";
        String partUrl = "/kommercheskaya_nedvizhimost/" +
                "sdam-ASgBAgICAUSwCNRW?cd=1&f=ASgBAQICAkSwCNRW9BKk2gEBQNQIRIysAb7_AbpZtlk" +
                "&pmin=17900000&proprofile=1&s=104";
        return baseUrl + city.getTitle() + partUrl;
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
