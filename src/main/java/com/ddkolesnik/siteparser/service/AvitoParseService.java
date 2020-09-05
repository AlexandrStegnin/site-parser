package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.utils.AdvertisementCategory;
import com.ddkolesnik.siteparser.utils.AdvertisementType;
import com.ddkolesnik.siteparser.utils.UrlUtils;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import javax.net.ssl.*;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@Service
public class AvitoParseService {

    private final AdvertisementService advertisementService;

    public AvitoParseService(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    /**
     * Собрать и записать информацию по объявлениям
     *
     * @param category категория объявления
     * @param advertisementType вид объявления
     * @param pageNumber номер страницы
     * @return список объявлений
     */
    public int parse(AdvertisementCategory category, AdvertisementType advertisementType, int pageNumber) {
        int i = pageNumber;
        List<String> links = new ArrayList<>();
        String url = "";
        while (pageNumber <= i) {
            if (category == AdvertisementCategory.TRADING_AREA && advertisementType == AdvertisementType.SALE) {
                url = UrlUtils.getTradingAreaSaleUrl(pageNumber);
            } else if (category == AdvertisementCategory.TRADING_AREA && advertisementType == AdvertisementType.RENT) {
                url = UrlUtils.getTradingAreaRentUrl(pageNumber);
            } else if (category == AdvertisementCategory.OTHER && advertisementType == AdvertisementType.SALE) {
                url = UrlUtils.getOtherCategoriesSaleUrl(pageNumber);
            } else if (category == AdvertisementCategory.OTHER && advertisementType == AdvertisementType.RENT) {
                url = UrlUtils.getOtherCategoriesRentUrl(pageNumber);
            }
            if (i == pageNumber) {
                i = calculateTotalPages(url);
            }
            log.info("Собираем ссылки со страницы {} из {}", pageNumber, i);
            links.addAll(getLinks(url));
            pageNumber++;
        }
        log.info("Итого собрано ссылок [{} шт]", links.size());
        return getAdvertisements(links, advertisementType);
    }

    /**
     * Собрать ссылки на объявления со страницы
     *
     * @param url ссылка на страницу
     * @return список ссылок на объявления
     */
    public List<String> getLinks(String url) {
        List<String> links = new ArrayList<>();
        Document document;
        try {
            enableSSLSocket();
            Thread.sleep(1_000);
            document = Jsoup.connect(url).get();
            document.select("a.snippet-link").forEach(a -> {
                Elements el = a.getElementsByAttributeValue("itemprop", "url");
                String href = el.select("a[href]").attr("href");
                if (!href.trim().isEmpty()) {
                    links.add(href.trim());
                }
            });
        } catch (IOException | NoSuchAlgorithmException | KeyManagementException | InterruptedException e) {
            log.error(String.format("Произошла ошибка: [%s]", e));
            return links;
        }
        return links;
    }

    /**
     * Получить информацию об объявлении со страницы
     *
     * @param url ссылка на страницу с объявлением
     * @param advertisementType вид объявления
     */
    public void parseAdvertisement(String url, AdvertisementType advertisementType) {
        String link = url;
        url = "https://www.avito.ru" + url;
        Advertisement advertisement;
        try {
            Thread.sleep(1_500);
            Document document = Jsoup.connect(url).get();

            advertisement = new Advertisement();
            advertisement.setAdvType(advertisementType.getTitle());
            advertisement.setTitle(getTitle(document));
            advertisement.setLink(link);
            advertisement.setArea(getArea(document));
            advertisement.setPrice(getPrice(document));
            advertisement.setAddress(getAddress(document));
            advertisement.setStations(getStations(document));
            advertisement.setDescription(getDescription(document));
            advertisement.setDateCreate(getDateCreate(document));
            setSellerInfo(document, advertisement);
        } catch (IOException | InterruptedException e) {
            log.error(String.format("Произошла ошибка: [%s]", e));
            return;
        }
        advertisementService.create(advertisement);
    }

    /**
     * Получить список объявлений из массива ссылок
     *
     * @param urls ссылки на объявления
     * @param advertisementType вид объявления
     */
    public int getAdvertisements(List<String> urls, AdvertisementType advertisementType) {
        int linksCount = urls.size();
        int counter = 0;
        while (counter < linksCount) {
            log.info("Собираем {} из {} объявлений", counter + 1, linksCount);
            parseAdvertisement(urls.get(counter), advertisementType);
            counter++;
        }
        return linksCount;
    }

    /**
     * Получаем название объявления
     *
     * @param document HTML страница
     * @return название объявления
     */
    private String getTitle(Document document) {
        String title = "";
        Element titleEl = document.select("span.title-info-title-text").first();
        if (titleEl != null) {
            title = titleEl.text();
        }
        return title;
    }

    /**
     * Получаем площадь объявления
     *
     * @param document HTML страница
     * @return площадь объявления
     */
    private String getArea(Document document) {
        String area = "";
        Elements areaEl = document.select("div.item-params");
        if (areaEl != null) {
            Element areaFirstEl = areaEl.select("span").first();
            if (areaFirstEl != null) {
                area = areaFirstEl.text().split(":")[1].replaceAll("[^\\d.]", "");
            }
        }
        return area;
    }

    /**
     * Получить стоимость объекта
     *
     * @param document HTML страница
     * @return стоимость объявления
     */
    private BigDecimal getPrice(Document document) {
        BigDecimal price = BigDecimal.ZERO;
        Element priceEl = document.select("span.js-item-price").select("[itemprop=price]").first();
        if (priceEl != null) {
            String priceStr = priceEl.text().replaceAll("\\s", "");
            price = new BigDecimal(priceStr);
        }
        return price;
    }

    /**
     * Получить адрес объекта
     *
     * @param document HTML страница
     * @return адрес объекта
     */
    private String getAddress(Document document) {
        String address = "";
        Element addressEl = document.select("span.item-address__string").first();
        if (addressEl != null) {
            address = addressEl.text().trim().replace(';', ' ');
        }
        return address;
    }

    /**
     * Получить список станций метро
     *
     * @param document HTML страница
     * @return станции метро возле объекта
     */
    private String getStations(Document document) {
        List<String> stations = new ArrayList<>();
        Element stationsArraySpan = document.selectFirst("span.item-address-georeferences");
        if (stationsArraySpan != null) {
            Elements stationsArrayItems = stationsArraySpan.select("span.item-address-georeferences-item");
            if (stationsArrayItems != null) {
                for (Element stationEl : stationsArrayItems) {
                    stations.add(stationEl.text().trim().replace(";", " "));
                }
            }
        }
        return String.valueOf(stations);
    }

    /**
     * Получить описание объявления
     *
     * @param document HTML страница
     * @return описание объявления
     */
    private String getDescription(Document document) {
        String description = "";
        Element descriptionEl = document.selectFirst("div.item-description");
        if (descriptionEl != null) {
            description = descriptionEl.text().trim().replace(";", " ");
        }
        return description;
    }

    /**
     * Получить дату создания объявления
     *
     * @param document HTML страница
     * @return дата создания объявления
     */
    private String getDateCreate(Document document) {
        String dateCreate = "";
        Element dateCreateEl = document.selectFirst("div.title-info-metadata-item-redesign");
        if (dateCreateEl != null) {
            dateCreate = dateCreateEl.text().replace("\n", "").trim();
        }
        return dateCreate;
    }

    /**
     * Добавить информацию об авторе в объявление
     *
     * @param document      HTML страница
     * @param advertisement объявление
     */
    private void setSellerInfo(Document document, Advertisement advertisement) {
        Element sellerInfoCol = document.select("div.seller-info-col").first();
        if (sellerInfoCol != null) {
            int elSize = sellerInfoCol.children().size();
            String sellerName = sellerInfoCol.child(0).text();
            String sellerType = sellerInfoCol.child(1).text();
            if (elSize > 2) {
                Elements children = sellerInfoCol.child(2).children();
                if (children.size() > 1) {
                    String sellerAdvComplete = children.get(1).text().replace("\n", "");
                    String sellerOnAvito = children.get(0).text().replace("\n", "");
                    advertisement.setSellerAdvComplete(sellerAdvComplete);
                    advertisement.setSellerOnAvito(sellerOnAvito);
                }
            }
            advertisement.setSellerName(sellerName);
            advertisement.setSellerType(sellerType);
        }
        setSellerAdvActual(document, advertisement);
    }

    /**
     * Добавить информацию о кол-ве актуальных объявлений
     *
     * @param document      HTML страница
     * @param advertisement объявление
     */
    @SuppressWarnings("unchecked")
    private void setSellerAdvActual(Document document, Advertisement advertisement) {
        Elements activeAdvDivs = document.getElementsByClass("seller-info-favorite-seller-buttons");
        if (activeAdvDivs != null) {
            String json = activeAdvDivs.select("[data-props]").attr("data-props");
            Gson gson = new Gson();
            Map<String, Object> asMap = gson.fromJson(json, Map.class);
            String sellerAdvActual = (String) asMap.getOrDefault("summary", "");
            advertisement.setSellerAdvActual(sellerAdvActual);
        }
    }

    /**
     * Посчитать приблизительное число страниц исходя из кол-ва объявлений,
     * делённых на 56 (среднее число объявлений на странице авито (03.09.2020))
     *
     * @param url ссылка на страницу с объявлениями по фильтру
     * @return приблизительное кол-во страниц
     */
    public int calculateTotalPages(String url) {
        int totalAdvertisements;
        int totalPages;
        try {
            Document document = Jsoup.connect(url).get();
            Element advCountEl = document.select("[data-marker=page-title/count]").first();
            if (advCountEl != null) {
                String advCount = advCountEl.text().trim().replaceAll("\\s", "");
                try {
                    totalAdvertisements = Integer.parseInt(advCount);
                    totalPages = totalAdvertisements / 56;
                    return ++totalPages;
                } catch (NumberFormatException e) {
                    log.error("Не удалось преобразовать полученный текст [{}] в кол-во объявлений. Ошибка: {}", advCount, e.getLocalizedMessage());
                    return 0;
                }
            }
            return 0;
        } catch (Exception e) {
            log.error(String.format("Произошла ошибка: %s", e.getLocalizedMessage()));
            return 0;
        }
    }

    private SSLSocketFactory socketFactory() {
        TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }

            public void checkClientTrusted(X509Certificate[] certs, String authType) {
            }

            public void checkServerTrusted(X509Certificate[] certs, String authType) {
            }
        }};

        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new SecureRandom());
            return sslContext.getSocketFactory();
        } catch (NoSuchAlgorithmException | KeyManagementException e) {
            throw new RuntimeException("Failed to create a SSL socket factory", e);
        }
    }

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
