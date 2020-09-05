package com.ddkolesnik.siteparser;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class SiteParserApplication {

    public static void main(String[] args) {
        SpringApplication.run(SiteParserApplication.class, args);
    }

}
