package com.ddkolesnik.siteparser.service;

import com.ddkolesnik.siteparser.model.Advertisement;
import com.ddkolesnik.siteparser.utils.AdvCategory;
import com.ddkolesnik.siteparser.utils.AdvertisementType;
import com.ddkolesnik.siteparser.utils.City;
import com.ddkolesnik.siteparser.utils.SubCategory;
import com.ddkolesnik.siteparser.utils.UrlUtils;
import com.google.gson.Gson;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class AvitoParseService {

  ScraperApiService scraperApiService;
  AdvertisementService advertisementService;

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
  public int parse(AdvCategory category, SubCategory subCategory, AdvertisementType advertisementType, City city,
                   LocalDate maxPublishDate) {
    log.info("Начинаем собирать [{}] :: [{}] :: [{}] :: [{}]", category.getTitle(), subCategory.getTitle(),
        advertisementType.getTitle(), city.getDescription());
    Map<String, LocalDate> links = new HashMap<>();
    String url = getUrl(category, subCategory, advertisementType, city);
    String pagePart = "&p=";
    int totalPages;
    if (Objects.isNull(maxPublishDate)) {
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
    Document document = getDocument(url);
    if (Objects.nonNull(document)) {
      Elements divs = document.select("div[data-marker=item]");
      divs.forEach(div -> {
        LocalDate advCreateDate = extractDate(div);
        if (Objects.nonNull(maxPublishDate) && Objects.nonNull(advCreateDate) && advCreateDate.isBefore(maxPublishDate)) {
          return;
        }
        Element urlEl = div.selectFirst("a[itemprop=url]");
        if (Objects.nonNull(urlEl)) {
          String href = urlEl.select("a[href]").attr("href");
          links.put(href.trim(), advCreateDate);
        }
      });
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
  public void parseAdvertisement(String url, AdvertisementType advertisementType, LocalDate publishDate, City city,
                                 AdvCategory category) {
    int retrieveCount = 5;
    String oldUrl = url;
    url = "https://avito.ru" + url;
    String link = url;
    Advertisement advertisement;
    Document document = getDocument(url);
    if (Objects.isNull(document)) {
      log.warn("Не удалось получить страницу [{}]", url);
      return;
    }
    while (document.text().toLowerCase(Locale.ROOT).contains("подозрительную") || retrieveCount == 0) {
      log.warn("Страница не доступна, пробуем повторить. {}", oldUrl);
      document = getDocument(oldUrl);
      retrieveCount--;
    }
    String address = getAddress(document);
    if (category == AdvCategory.COMMERCIAL_PROPERTY) {
      if (!checkAddress(address, city)) {
        log.warn("Адресс не валидный. [{}] :: [{}]", city.getDescription(), address);
        return;
      }
    }
    String title = getTitle(document);
    advertisement = Advertisement.builder()
        .advType(advertisementType.getTitle())
        .title(title)
        .link(link)
        .area(getArea(document))
        .price(getPrice(document))
        .address(address)
        .stations(getStations(document))
        .description(getDescription(document))
        .dateCreate(getDateCreate(document))
        .publishDate(publishDate)
        .city(city.getDescription())
        .category(category.getTitle())
        .build();

    setSellerInfo(document, advertisement);
    advertisementService.create(advertisement);
    log.info("Сохранили объявление: {}", advertisement.getId());
  }

  /**
   * Получить список объявлений из массива ссылок
   *
   * @param urls              ссылки на объявления
   * @param advertisementType вид объявления
   * @param city              город
   * @param category          категория объявления
   */
  public int getAdvertisements(Map<String, LocalDate> urls, AdvertisementType advertisementType, City city,
                               AdvCategory category) {
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
    if (Objects.nonNull(titleEl)) {
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
    Elements areas = areaEl.select("span");

    if (areas.size() == 6) {
      area = areaEl.select("li").text().split(":")[1].replaceAll("[^\\d.]", "");
    } else {
      Element areaFirstEl = areaEl.select("span").first();
      if (Objects.nonNull(areaFirstEl)) {
        String[] areaParts = areaFirstEl.text().split(":");
        if (areaParts.length > 1) {
          area = areaParts[1].replaceAll("[^\\d.]", "");
          if (area.endsWith(".")) {
            area = area.substring(0, area.length() - 1);
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
    if (Objects.nonNull(priceEl)) {
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
    if (Objects.nonNull(addressEl)) {
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
    if (Objects.nonNull(stationsArraySpan)) {
      Elements stationsArrayItems = stationsArraySpan.select("span.item-address-georeferences-item");
      for (Element stationEl : stationsArrayItems) {
        stations.add(stationEl.text().trim());
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
    if (Objects.nonNull(descriptionEl)) {
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
    if (Objects.nonNull(dateCreateEl)) {
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
    if (Objects.nonNull(sellerInfoCol)) {
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
    String json = activeAdvDivs.select("[data-props]").attr("data-props");
    Gson gson = new Gson();
    Map<String, Object> asMap = gson.fromJson(json, Map.class);
    if (asMap != null) {
      sellerAdvActual = (String) asMap.getOrDefault("summary", "");
    }
    advertisement.setSellerAdvActual(sellerAdvActual);
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
      if (Objects.isNull(document)) {
        return 0;
      }
      Element pageCountDiv = document.getElementsByClass("pagination-pages").first();
      if (Objects.nonNull(pageCountDiv)) {
        Element pageCountHref = pageCountDiv.getElementsByClass("pagination-pages").last();
        if (Objects.nonNull(pageCountHref)) {
          Element hrefElement = pageCountHref.getElementsByAttribute("href").last();
          if (Objects.nonNull(hrefElement)) {
            String pCount = hrefElement.getElementsByAttribute("href").get(0).attr("href")
                .split("=")[1].split("&")[0];
            try {
              totalPages = Integer.parseInt(pCount);
              return totalPages;
            } catch (NumberFormatException e) {
              log.error("Не удалось преобразовать полученный текст [{}] в кол-во объявлений. Ошибка: {}", pCount,
                  e.getLocalizedMessage());
              return 0;
            }
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
  private Document getDocument(String url) {
    int retrieveCount = 5;
    Document document = scraperApiService.getDocument(url);
    while (document.text().toLowerCase(Locale.ROOT).contains("подозрительная") && retrieveCount > 0) {
      log.warn("Страница не доступна, пробуем повторить. {}", url);
      document = scraperApiService.getDocument(url);
      retrieveCount--;
    }
    return document;
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
    if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.TRADING_AREA &&
        type == AdvertisementType.SALE) {
      return UrlUtils.getTradingAreaSaleUrl(city);
    } else if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.TRADING_AREA &&
        type == AdvertisementType.RENT) {
      return UrlUtils.getTradingAreaRentUrl(city);
    } else if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.OTHER &&
        type == AdvertisementType.SALE) {
      return UrlUtils.getOtherCategoriesSaleUrl(city);
    } else if (category == AdvCategory.COMMERCIAL_PROPERTY && subCategory == SubCategory.OTHER &&
        type == AdvertisementType.RENT) {
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
    if (dateCreate.isEmpty()) {
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
      log.error("Произошла ошибка: {}", e.getLocalizedMessage());
      return null;
    }
  }

  /**
   * Проверить, может объявление создавалось несколько секунд назад
   *
   * @param strDate дата создания объявления в виде строки (3 секунды назад)
   * @return результат проверки
   */
  private boolean checkSecondsBefore(String strDate) {
    Pattern pattern = Pattern.compile("(секунд([ыу])) назад");
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
