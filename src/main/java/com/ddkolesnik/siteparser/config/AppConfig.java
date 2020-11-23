package com.ddkolesnik.siteparser.config;

import com.gargoylesoftware.htmlunit.BrowserVersion;
import com.gargoylesoftware.htmlunit.SilentCssErrorHandler;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebClientOptions;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author Alexandr Stegnin
 */

@Configuration
public class AppConfig {

    @Bean
    public WebClient webClient() {
        WebClient webClient = new WebClient(BrowserVersion.FIREFOX_78);
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

}
