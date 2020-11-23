package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.utils.*;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@Service
public class AvitoParseService {

    private final AdvertisementService advertisementService;

    private final WebClient webClient;

    public AvitoParseService(AdvertisementService advertisementService, WebClient webClient) {
        this.advertisementService = advertisementService;
        this.webClient = webClient;
    }

    /**
     * Собрать и записать информацию по объявлениям
     *
     * @param category          категория объявления
     * @param subCategory       подкатегория
     * @param city              город
     * @param advertisementType вид объявления
     * @param maxPublishDate    дата последней публикации в базе данных
     * @return список объявлений
     */
    public int parse(AdvCategory category, SubCategory subCategory, AdvertisementType advertisementType, City city, LocalDate maxPublishDate) {
        log.info("Начинаем собирать [{}] :: [{}] :: [{}]", category.getTitle(), advertisementType.getTitle(), city.getDescription());
        Map<String, LocalDate> links = new HashMap<>();
        String url = getUrl(category, subCategory, advertisementType, city);
        String pagePart = "&p=";
        int totalPages;
        if (maxPublishDate == null) {
            totalPages = getTotalPages(url);
        } else {
            totalPages = 3;
        }
        int pageNumber = 1;
        while (pageNumber <= totalPages) {
            log.info("Собираем ссылки со страницы {} из {}", pageNumber, totalPages);
            links.putAll(getLinks(url.concat(pagePart).concat(String.valueOf(pageNumber)), maxPublishDate));
            pageNumber++;
        }
        log.info("Итого собрано ссылок [{} шт]", links.size());
        return getAdvertisements(links, advertisementType, city, category);
    }

    /**
     * Собрать ссылки на объявления со страницы
     *
     * @param url            ссылка на страницу
     * @param maxPublishDate дата последней публикации в базе данных
     * @return список ссылок на объявления
     */
    public Map<String, LocalDate> getLinks(String url, LocalDate maxPublishDate) {
        Map<String, LocalDate> links = new HashMap<>();
        Document document;
        try {
            document = getDocument(url);
            Elements aSnippetLinks = document.select("a.snippet-link");
            for (Element element : aSnippetLinks) {
                Elements el = element.getElementsByAttributeValue("itemprop", "url");
                String href = el.select("a[href]").attr("href");
                if (!href.trim().isEmpty()) {
                    LocalDate advCreateDate = extractDate(element);
                    if (maxPublishDate != null && advCreateDate != null && advCreateDate.isBefore(maxPublishDate)) {
                        return links;
                    }
                    links.put(href.trim(), advCreateDate);
                }
            }
        } catch (HttpStatusException e) {
            waiting(e);
        } catch (IOException e) {
            log.error(String.format("Произошла ошибка: [%s]", e));
            return links;
        }
        return links;
    }

    /**
     * Получить информацию об объявлении со страницы
     *
     * @param url               ссылка на страницу с объявлением
     * @param advertisementType вид объявления
     * @param publishDate       дата публикации объявления
     * @param city              город
     * @param category          категория объявления
     */
    public void parseAdvertisement(String url, AdvertisementType advertisementType, LocalDate publishDate, City city, AdvCategory category) {
        url = "https://avito.ru" + url;
        String link = url;
        Advertisement advertisement;
        try {
            Document document = getDocument(url);
            String address = getAddress(document);
            if (!checkAddress(address, city)) {
                return;
            }
            String title = getTitle(document);
            if (title == null) {
                return;
            }
            advertisement = new Advertisement();
            advertisement.setAdvType(advertisementType.getTitle());
            advertisement.setTitle(getTitle(document));
            advertisement.setLink(link);
            advertisement.setArea(getArea(document));
            advertisement.setPrice(getPrice(document));
            advertisement.setAddress(address);
            advertisement.setStations(getStations(document));
            advertisement.setDescription(getDescription(document));
            advertisement.setDateCreate(getDateCreate(document));
            advertisement.setPublishDate(publishDate);
            advertisement.setCity(city.getDescription());
            advertisement.setCategory(category.getTitle());
            setSellerInfo(document, advertisement);
        } catch (HttpStatusException e) {
            waiting(e);
            return;
        } catch (IOException e) {
            log.error(String.format("Произошла ошибка: [%s]", e));
            return;
        }
        advertisementService.create(advertisement);
    }

    /**
     * Метод для ожидания, в случае, если сервер сказал, что мы "спамим"
     *
     * @param e ошибка
     */
    private void waiting(HttpStatusException e) {
        if (e.getStatusCode() == 429) {
            log.error("Слишком много запросов {}", e.getLocalizedMessage());
            log.info("Засыпаем на 60 мин для обхода блокировки");
            try {
                Thread.sleep(60 * 1000 * 60);
            } catch (InterruptedException exception) {
                log.error(String.format("Произошла ошибка: [%s]", exception));
            }
        }
    }

    /**
     * Получить список объявлений из массива ссылок
     *
     * @param urls              ссылки на объявления
     * @param advertisementType вид объявления
     * @param city              город
     * @param category          категория объявления
     */
    public int getAdvertisements(Map<String, LocalDate> urls, AdvertisementType advertisementType, City city, AdvCategory category) {
        int linksCount = urls.size();
        AtomicInteger counter = new AtomicInteger(0);
        urls.forEach((url, date) -> {
            log.info("Собираем {} из {} объявлений", counter.get() + 1, linksCount);
            parseAdvertisement(url, advertisementType, date, city, category);
            counter.getAndIncrement();
        });
        return linksCount;
    }

    /**
     * Получаем название объявления
     *
     * @param document HTML страница
     * @return название объявления
     */
    private String getTitle(Document document) {
        String title = null;
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
        String area = null;
        Elements areaEl = document.select("div.item-params");
        if (areaEl != null) {
            Elements areas = areaEl.select("span");
            if (areas != null && areas.size() > 0) {
                Element areaFirstEl = areaEl.select("span").first();
                if (areaFirstEl != null) {
                    String[] areaParts = areaFirstEl.text().split(":");
                    if (areaParts.length > 1) {
                        area = areaParts[1].replaceAll("[^\\d.]", "");
                    }
                }
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
        String address = null;
        Element addressEl = document.select("span.item-address__string").first();
        if (addressEl != null) {
            address = addressEl.text().trim();
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
                    stations.add(stationEl.text().trim());
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
            description = descriptionEl.text().trim();
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
        String sellerAdvActual = "";
        if (activeAdvDivs != null) {
            String json = activeAdvDivs.select("[data-props]").attr("data-props");
            Gson gson = new Gson();
            Map<String, Object> asMap = gson.fromJson(json, Map.class);
            if (asMap != null) {
                sellerAdvActual = (String) asMap.getOrDefault("summary", "");
            }
            advertisement.setSellerAdvActual(sellerAdvActual);
        }
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
    public Document getDocument(String url) throws IOException {
        long timer = 6_000;
        try {
            Thread.sleep(timer);
        } catch (InterruptedException e) {
            log.error("Произошла ошибка: " + e.getLocalizedMessage());
        }
        HtmlPage page = webClient.getPage(url);
        return Jsoup.parse(page.asXml());
    }

    /**
     * Получить ссылку для обработки в зависимости от фильтров
     *
     * @param category    категория объявления
     * @param subCategory подкатегория объявления
     * @param type        вид объявления
     * @param city        город объявления
     * @return ссылка
     */
    private String getUrl(AdvCategory category, SubCategory subCategory, AdvertisementType type, City city) {
        String url = "";
        if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.TRADING_AREA && type == AdvertisementType.SALE) {
            return UrlUtils.getTradingAreaSaleUrl(city);
        } else if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.TRADING_AREA && type == AdvertisementType.RENT) {
            return UrlUtils.getTradingAreaRentUrl(city);
        } else if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.OTHER && type == AdvertisementType.SALE) {
            return UrlUtils.getOtherCategoriesSaleUrl(city);
        } else if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.OTHER && type == AdvertisementType.RENT) {
            return UrlUtils.getOtherCategoriesRentUrl(city);
        }
        if (category == AdvCategory.HOUSE_COUNTRY_HOUSE_COTTAGE && type == AdvertisementType.SALE) {
            return UrlUtils.getHouseCountryHouseCottageSaleUrl(city);
        } else if (category == AdvCategory.HOUSE_COUNTRY_HOUSE_COTTAGE && type == AdvertisementType.RENT) {
            return UrlUtils.getHouseCountryHouseCottageRentUrl(city);
        }
        return url;
    }

    /**
     * Получить дату публикации объявления
     *
     * @param element HTML элемент
     * @return дата публикации
     */
    private LocalDate extractDate(Element element) {
        Element divDateEl = element.parent().parent().parent().selectFirst("div.snippet-date-info");
        if (divDateEl == null) {
            return null;
        }
        String dateCreate = divDateEl.attr("data-tooltip");
        if (dateCreate == null || dateCreate.isEmpty()) {
            return null;
        }
        SimpleDateFormat format = new SimpleDateFormat("dd MMM hh:mm", Locale.forLanguageTag("RU"));
        try {
            Date parsedDate = format.parse(dateCreate);
            LocalDate finalDate = parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return LocalDate.of(LocalDate.now().getYear(), finalDate.getMonth(), finalDate.getDayOfMonth());
        } catch (ParseException e) {
            log.error("Произошла ошибка: {}", e.getLocalizedMessage());
            return null;
        }
    }

    /**
     * Проверить адрес, должен содержать в себе Московская область, г Москва/Свердловская обл, г Екатеринбург/Тюменская обл, г Тюмень
     *
     * @param address адресс для проверки
     * @param city    город для получения регулярного выражения
     * @return результат проверки
     */
    private boolean checkAddress(String address, City city) {
        if (address == null) {
            return true;
        }
        if (checkArea(address, city)) {
            return checkCity(address, city);
        }
        return true;
    }

    /**
     * Проверить область по шаблону
     *
     * @param address адрес
     * @param city    город
     * @return результат
     */
    private boolean checkArea(String address, City city) {
        Pattern pattern = Pattern.compile(city.getPattern());
        Matcher matcher = pattern.matcher(address.toLowerCase());
        return matcher.find();
    }

    /**
     * Проверить город по шаблону
     *
     * @param address адрес
     * @param city    город
     * @return результат
     */
    private boolean checkCity(String address, City city) {
        String cityName = city.getDescription().toLowerCase();
        String template = "(%s)";
        String cityPattern = String.format(template, cityName);
        Pattern pattern = Pattern.compile(cityPattern);
        Matcher matcher = pattern.matcher(address.toLowerCase());
        return matcher.find();
    }

}
