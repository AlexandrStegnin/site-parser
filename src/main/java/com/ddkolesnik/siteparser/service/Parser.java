package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.utils.AdvertisementCategory;
import com.ddkolesnik.siteparser.utils.AdvertisementType;
import com.ddkolesnik.siteparser.utils.City;
import com.ddkolesnik.siteparser.utils.UrlUtils;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@Service
public class Parser {

    private final AdvertisementService advertisementService;

    public Parser(AdvertisementService advertisementService) {
        this.advertisementService = advertisementService;
    }

    /*
    1. Получить ссылку на страницу с категорями объявления
    2. Найти кол-во страниц
    3. Пройтись по всем страницам и собрать краткую инфу

     */

    public int parse(AdvertisementCategory category, AdvertisementType type, City city) throws IOException {
        List<Advertisement> advertisements = new ArrayList<>();
        String url = getUrl(category, type, city);
        int totalPages = getTotalPages(url);
        int counter = 1;
        while (counter <= totalPages) {
            String pagePart = "&p=" + counter;
            advertisements.addAll(getAdvertisements(url.concat(pagePart), type, city));
            counter++;
        }
        return advertisements.size();
    }

    /**
     * Получить список объявлений
     *
     * @param url ссылка для сбора
     * @param type вид объявления
     * @param city город объявления
     * @return список объявлений
     * @throws IOException документ (ссылка) не найден
     */
    private List<Advertisement> getAdvertisements(String url, AdvertisementType type, City city) throws IOException {
        List<Advertisement> advertisements = new ArrayList<>();
        Document document = getDocument(url);
        Elements adsArray = document.select("div.description.item_table-description");
        for (Element ad : adsArray) {
            String title = getTitle(ad);
            String area = extractArea(title);

            Advertisement advertisement = new Advertisement();
            advertisement.setTitle(title);
            advertisement.setArea(area);
            advertisement.setLink(getLink(ad));
            advertisement.setPrice(getPrice(ad));
            advertisement.setAddress(getAddress(ad));
            advertisement.setStations(getStation(ad));
            advertisement.setSellerName(getSellerName(ad));
            advertisement.setDateCreate(getDateCreate(ad));
            advertisement.setAdvType(type.getTitle());
            advertisement.setCity(city.getDescription());
            advertisements.add(advertisement);
        }
        return advertisements;
    }

    /**
     * Получить площадь из названия
     *
     * @param title название
     * @return площадь
     */
    private String extractArea(String title) {
        String area = "";
        Pattern pattern = Pattern.compile("\\d+\\.*\\d+\\s[\\W]\u00B2|\\d+\\.*\\d+\\s[\\W][2]");
        Matcher matcher = pattern.matcher(title);
        while (matcher.find()) {
            area = title.substring(matcher.start(), matcher.end());
        }
        return area;
    }

    /**
     * Получить кол-во страниц
     *
     * @param url ссылка на страницу
     * @return кол-во страниц
     */
    private int getTotalPages(String url) {
        int totalPages;
        try {
            Document document = getDocument(url);
            Element pageCountDiv = document.getElementsByClass("pagination-pages").first();
            if (pageCountDiv != null) {
                Element pageCountHref = pageCountDiv.getElementsByClass("pagination-pages").last();
                if (pageCountHref != null) {
                    String pCount = pageCountHref.getElementsByAttribute("href").last()
                            .getElementsByAttribute("href").get(0).attr("href")
                            .split("=")[1].split("&")[0];
                    try {
                        totalPages = Integer.parseInt(pCount);
                        return totalPages;
                    } catch (NumberFormatException e) {
                        log.error("Не удалось преобразовать полученный текст [{}] в кол-во объявлений. Ошибка: {}", pCount, e.getLocalizedMessage());
                        return 0;
                    }
                }

            } else {
                return 1;
            }
            return 0;
        } catch (Exception e) {
            log.error(String.format("Произошла ошибка: %s", e.getLocalizedMessage()));
            return 0;
        }
    }

    /**
     * Получить объект страницы HTML
     *
     * @param url адрес страницы
     * @return объект страницы HTML
     * @throws IOException любая ошибка, связанная с открытием адреса страницы
     */
    private Document getDocument(String url) throws IOException {
        return Jsoup.connect(url).referrer(url).get();
    }

    /**
     * Получаем название объявления
     *
     * @param element HTML элемент
     * @return название объявления
     */
    private String getTitle(Element element) {
        String title = null;
        Element snippetTitleRow = element.selectFirst("div.snippet-title-row");
        if (snippetTitleRow != null) {
            Elements titleLinks = snippetTitleRow.select("a.snippet-link");
            if (titleLinks.size() > 0) {
                title = snippetTitleRow.select("a.snippet-link").get(0).attr("title").trim();
            }
        }
        return title;
    }

    /**
     * Получаем ссылку объявления
     *
     * @param element HTML элемент
     * @return ссылка объявления
     */
    private String getLink(Element element) {
        String prefix = "https://www.avito.ru/";
        String link = null;
        Element snippetTitleRow = element.selectFirst("div.snippet-title-row");
        if (snippetTitleRow != null) {
            Elements titleLinks = snippetTitleRow.select("a.snippet-link");
            if (titleLinks.size() > 0) {
                link = prefix.concat(snippetTitleRow.select("a.snippet-link").get(0).attr("href").trim());
            }
        }
        return link;
    }

    /**
     * Получить стоимость объекта
     *
     * @param element HTML элемент
     * @return стоимость объявления
     */
    private BigDecimal getPrice(Element element) {
        BigDecimal price = BigDecimal.ZERO;
        Element snippetPriceRow = element.selectFirst("div.snippet-price-row");
        if (snippetPriceRow != null) {
            Elements priceMeta = snippetPriceRow.select("meta");
            if (priceMeta.size() > 1) {
                String priceStr = priceMeta.get(1).attr("content");
                try {
                    price = new BigDecimal(priceStr);
                } catch (NumberFormatException e) {
                    log.error("Невозможно преобразовать стоимость из строки [{}]", priceStr);
                    return price;
                }
            }
        }
        return price;
    }

    /**
     * Получить адрес объекта
     *
     * @param element HTML элемент
     * @return адрес объекта
     */
    private String getAddress(Element element) {
        String address = null;
        Element addressRow = element.selectFirst("div.address");
        if (addressRow != null) {
            Element spanAddress = addressRow.selectFirst("span.item-address__string");
            if (spanAddress != null) {
                address = spanAddress.text();
            }
        }
        return address;
    }

    /**
     * Получить станцию метро
     *
     * @param element HTML элемент
     * @return станция метро возле объекта
     */
    private String getStation(Element element) {
        String station = null;
        Element addressRow = element.selectFirst("div.address");
        if (addressRow != null) {
            Element metroRow = addressRow.selectFirst("span.item-address-georeferences-item");
            if (metroRow != null) {
                station = metroRow.text();
            }
        }
        return station;
    }

    /**
     * Получить название автора объявления
     *
     * @param element HTML элемент
     * @return название автора
     */
    private String getSellerName(Element element) {
        String sellerName = null;
        Element dataRow = element.selectFirst("div.data");
        if (dataRow != null) {
            Element sellerElement = dataRow.selectFirst("p");
            if (sellerElement != null) {
                sellerName = sellerElement.text();
            }
        }
        return sellerName;
    }

    /**
     * Получить дату создания объявления
     *
     * @param element HTML элемент
     * @return дата создания объявления
     */
    private String getDateCreate(Element element) {
        String dateCreate = null;
        Element dataRow = element.selectFirst("div.data");
        if (dataRow != null) {
            Element dateElement = dataRow.selectFirst("div.snippet-date-info");
            if (dateElement != null) {
                dateCreate = dateElement.attr("data-tooltip");
            }
        }
        return dateCreate;
    }

    /**
     * Получить ссылку для обработки в зависимости от фильтров
     *
     * @param category категория объявления
     * @param type вид объявления
     * @param city город объявления
     * @return ссылка
     */
    private String getUrl(AdvertisementCategory category, AdvertisementType type, City city) {
        String url = "";
        if (category == AdvertisementCategory.TRADING_AREA && type == AdvertisementType.SALE) {
            url = UrlUtils.getTradingAreaSaleUrl(city);
        } else if (category == AdvertisementCategory.TRADING_AREA && type == AdvertisementType.RENT) {
            url = UrlUtils.getTradingAreaRentUrl(city);
        } else if (category == AdvertisementCategory.OTHER && type == AdvertisementType.SALE) {
            url = UrlUtils.getOtherCategoriesSaleUrl(city);
        } else if (category == AdvertisementCategory.OTHER && type == AdvertisementType.RENT) {
            url = UrlUtils.getOtherCategoriesRentUrl(city);
        }
        return url;
    }

}
