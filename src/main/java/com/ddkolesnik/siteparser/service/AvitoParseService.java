package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.utils.*;
import com.gargoylesoftware.htmlunit.AjaxController;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

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
        log.info("Начинаем собирать [{}] :: [{}] :: [{}] :: [{}]", category.getTitle(), subCategory.getTitle(), advertisementType.getTitle(), city.getDescription());
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
        document = getDocument(url);
        if (document != null) {
            Elements divs = document.select("div[data-marker=item]");
            for (Element element : divs) {
                LocalDate advCreateDate = extractDate(element);
                if (maxPublishDate != null && advCreateDate != null && advCreateDate.isBefore(maxPublishDate)) {
                    return links;
                }
                String href = element.selectFirst("a[itemprop=url]").select("a[href]").attr("href");
                links.put(href.trim(), advCreateDate);
            }
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
        Document document = getDocument(url);
        if (document == null) {
            log.warn("Не удалось получить страницу [{}]", url);
            return;
        }
        String address = getAddress(document);
        if (category == AdvCategory.COMMERCIAL_PROPERTY) {
            if (!checkAddress(address, city)) {
                log.warn("Адресс не валидный. [{}] :: [{}]", city.getDescription(), address);
                return;
            }
        }
        String title = getTitle(document);
        if (title == null) {
            log.warn("Отсутствует название объявления [{}]", url);
            return;
        }
        advertisement = new Advertisement();
        advertisement.setAdvType(advertisementType.getTitle());
        advertisement.setTitle(title);
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
        advertisementService.create(advertisement);
        log.info("Сохранили объявление: {}", advertisement.getId());
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

    private void waiting() {
        log.info("Засыпаем на 60 мин для обхода блокировки");
        try {
            Thread.sleep(60 * 1000 * 60);
        } catch (InterruptedException exception) {
            log.error(String.format("Произошла ошибка: [%s]", exception));
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
            if (areas != null) {
                if (areas.size() == 6) {
                    area = areaEl.select("li").text().split(":")[1].replaceAll("[^\\d.]", "");
                } else {
                    Element areaFirstEl = areaEl.select("span").first();
                    if (areaFirstEl != null) {
                        String[] areaParts = areaFirstEl.text().split(":");
                        if (areaParts.length > 1) {
                            area = areaParts[1].replaceAll("[^\\d.]", "");
                            if (area.endsWith(".")) {
                                area = area.substring(0, area.length() - 1);
                            }
                        }
                    }
                }
            } else {
                Element liArea = areaEl.select("li.item-params-list-item").first();
                if (liArea != null) {
                    area = liArea.text().replaceAll("[^\\d.]", "");
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
            if (document == null) {
                return 0;
            }
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
     */
    public Document getDocument(String url) {
        long timer = 6_000;
        try {
            Thread.sleep(timer);
        } catch (InterruptedException e) {
            log.error("Произошла ошибка: " + e.getLocalizedMessage());
        }
        HtmlPage page;
        try {
            webClient.setAjaxController(new AjaxController() {
                @Override
                public boolean processSynchron(HtmlPage page, WebRequest request, boolean async) {
                    return true;
                }
            });
            page = webClient.getPage(url);
            if (page.asText().toLowerCase().contains("доступ временно заблокирован")) {
                waiting();
            }
            for (int i = 0; i < 20; i++) {
                if (page.asXml().contains("data-marker=\"item\"")) {
                    break;
                }
                synchronized (page) {
                    page.wait(500);
                }
            }

//            webClient.waitForBackgroundJavaScript(10 * 1000);
            return Jsoup.parse(page.asXml());
        } catch (HttpStatusException e) {
            waiting(e);
        } catch (Exception e) {
            log.error("Произошла ошибка: " + e.getLocalizedMessage());
        }
        return null;
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
        } else if (category == AdvCategory.HOUSE_COUNTRY_HOUSE_COTTAGE && subCategory == SubCategory.OTHER) {
            return UrlUtils.getHouseCountryHouseCottageUrl(city, type);
        } else if (category == AdvCategory.STEAD && subCategory == SubCategory.OTHER) {
            return UrlUtils.getSteadUrl(city, type);
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
        Element divDateEl = element.selectFirst("div[data-marker=item-date]");
        if (divDateEl == null) {
            return null;
        }
        String dateCreate = divDateEl.text();
        if (dateCreate == null || dateCreate.isEmpty()) {
            return null;
        }
        if (checkHoursBefore(dateCreate) || checkMinutesBefore(dateCreate) || checkSecondsBefore(dateCreate)) {
            return LocalDate.now();
        }
        if (checkDaysBefore(dateCreate)) {
            return parseDaysBefore(dateCreate);
        }
        if (checkWeeksBefore(dateCreate)) {
            return parseWeeksBefore(dateCreate);
        }
        SimpleDateFormat format = new SimpleDateFormat("dd MMM hh:mm", Locale.forLanguageTag("RU"));
        try {
            Date parsedDate = format.parse(dateCreate);
            LocalDate finalDate = parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            return LocalDate.of(LocalDate.now().getYear(), finalDate.getMonth(), finalDate.getDayOfMonth());
        } catch (ParseException e) {
            SimpleDateFormat fullFormat = new SimpleDateFormat("dd MMMM yyyy", Locale.forLanguageTag("RU"));
            try {
                Date parsedDate = fullFormat.parse(dateCreate);
                LocalDate finalDate = parsedDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                return LocalDate.of(LocalDate.now().getYear(), finalDate.getMonth(), finalDate.getDayOfMonth());
            } catch (ParseException ex) {
                log.error("Произошла ошибка: {}", ex.getLocalizedMessage());
                return null;
            }
        }
    }

    /**
     * Проверить, может объявление создавалось несколько секунд назад
     *
     * @param strDate дата создания объявления в виде строки (3 секунды назад)
     * @return результат проверки
     */
    private boolean checkSecondsBefore(String strDate) {
        Pattern pattern = Pattern.compile("(секунд([ыу])?) назад");
        Matcher matcher = pattern.matcher(strDate);
        return matcher.find();
    }

    /**
     * Проверить, может объявление создавалось несколько минут назад
     *
     * @param strDate дата создания объявления в виде строки (3 минуты назад)
     * @return результат
     */
    private boolean checkMinutesBefore(String strDate) {
        Pattern pattern = Pattern.compile("минут([аыу])? назад");
        Matcher matcher = pattern.matcher(strDate);
        return matcher.find();
    }

    /**
     * Проверить, может объявление создавалось несколько часов назад
     *
     * @param strDate дата создания объявления в виде строки (3 часа назад)
     * @return результат
     */
    private boolean checkHoursBefore(String strDate) {
        Pattern pattern = Pattern.compile("час(ов|а)? назад");
        Matcher matcher = pattern.matcher(strDate);
        return matcher.find();
    }

    /**
     * Проверить, может объявление создавалось несколько дней назад
     *
     * @param strDate дата создания объявления в виде строки (3 дня назад)
     * @return результат проверки
     */
    private boolean checkDaysBefore(String strDate) {
        Pattern pattern = Pattern.compile("(день|дней|дня) назад");
        Matcher matcher = pattern.matcher(strDate);
        return matcher.find();
    }

    /**
     * Проверить, может объявление создавалось несколько недель назад
     *
     * @param strDate дата создания объявления в виде строки (3 недели назад)
     * @return результат проверки
     */
    private boolean checkWeeksBefore(String strDate) {
        Pattern pattern = Pattern.compile("(недел([ьяию])) назад");
        Matcher matcher = pattern.matcher(strDate);
        return matcher.find();
    }

    /**
     * Получить дату из строки формата (N дней назад)
     *
     * @param dateCreate дата создания объявления в виде строки (3 дня назад)
     * @return дата
     */
    private LocalDate parseDaysBefore(String dateCreate) {
        try {
            LocalDate date = LocalDate.now();
            String minusDays = dateCreate.replaceAll("\\D", "");
            return date.minusDays(Integer.parseInt(minusDays));
        } catch (NumberFormatException e) {
            log.warn("Ошибка получения даты: " + dateCreate);
            return null;
        }
    }

    /**
     * Получить дату из строки формата (N недель назад)
     *
     * @param dateCreate дата создания объявления в виде строки (3 недели назад)
     * @return дата
     */
    private LocalDate parseWeeksBefore(String dateCreate) {
        try {
            LocalDate date = LocalDate.now();
            String minusWeeks = dateCreate.replaceAll("\\D", "");
            return date.minusWeeks(Integer.parseInt(minusWeeks));
        } catch (NumberFormatException e) {
            log.warn("Ошибка получения даты: " + dateCreate);
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
            return true;
        }
        return checkCity(address, city);
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
