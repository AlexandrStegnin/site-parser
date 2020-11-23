package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum  AdvCategory {

    COMMERCIAL_PROPERTY("Коммерческая недвижимость", "kommercheskaya_nedvizhimost"),
    HOUSE_COUNTRY_HOUSE_COTTAGE("Дома, дачи, коттеджи", "doma_dachi_kottedzhi");

    private final String title;

    private final String url;

    AdvCategory(String title, String url) {
        this.title = title;
        this.url = url;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
