package com.ddkolesnik.siteparser.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Alexandr Stegnin
 */

@Slf4j
@RestController
public class ShutdownController implements ApplicationContextAware {

    private ApplicationContext context;

    @GetMapping("/shutdown")
    public void shutdownContext() {
        log.info("Останавливаем приложение...");
        ((ConfigurableApplicationContext) context).close();
    }

    @Override
    public void setApplicationContext(ApplicationContext ctx) throws BeansException {
        this.context = ctx;
    }

}
