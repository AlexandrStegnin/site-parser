package com.ddkolesnik.siteparser.config;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import com.scraperapi.ScraperApiClient;
import kong.unirest.Unirest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alexandr Stegnin
 */

@Configuration
public class AppConfig {

    @Value("${scraper.api.key}")
    String scraperApiKey;

    @Bean
    public WebClient webClient() {
        WebClient webClient = new WebClient(BrowserVersion.INTERNET_EXPLORER);
        webClient.setCssErrorHandler(new SilentCssErrorHandler());
        configureOptions(webClient);
        return webClient;
    }

    private void configureOptions(WebClient client) {
        WebClientOptions options = client.getOptions();
        options.setTimeout(0);
        options.setCssEnabled(true);
        options.setJavaScriptEnabled(false);
        options.setThrowExceptionOnScriptError(false);
        options.setPrintContentOnFailingStatusCode(false);
        options.setThrowExceptionOnFailingStatusCode(false);
    }

    @Bean
    public ScraperApiClient scraperApiClient() {
        Unirest.config().socketTimeout(0).connectTimeout(0);
        return new ScraperApiClient(scraperApiKey);
    }

}
