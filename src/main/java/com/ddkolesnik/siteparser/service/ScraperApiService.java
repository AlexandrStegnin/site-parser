package com.ddkolesnik.siteparser.service;

import com.scraperapi.ScraperApiClient;
import kong.unirest.Unirest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;

/**
 * @author Aleksandr Stegnin on 22.07.2021
 */
@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(makeFinal = true, level = AccessLevel.PRIVATE)
public class ScraperApiService {

  ScraperApiClient client;

  public Document getDocument(String url) {
    reset();
    return Jsoup.parseBodyFragment(getResponse(url));
  }

  public String getResponse(String url) {
    return client.get(url).timeout(0).render(true).result();
  }

  public void reset() {
    Unirest.config().reset();
  }

}

