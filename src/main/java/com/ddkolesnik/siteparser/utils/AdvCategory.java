package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum  AdvCategory {

    COMMERCIAL_PROPERTY("Коммерческая недвижимость", "kommercheskaya_nedvizhimost"),
    HOUSE_COUNTRY_HOUSE_COTTAGE("Дома, дачи, коттеджи", "doma_dachi_kottedzhi"),
    STEAD("Земельные участки", "zemelnye_uchastki");

    private final String title;

    private final String category;

    AdvCategory(String title, String category) {
        this.title = title;
        this.category = category;
    }

    public String getTitle() {
        return title;
    }

    public String getCategory() {
        return category;
    }
}
