package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum AdvertisementCategory {

    TRADING_AREA(1, "Торговые площади"),
    OTHER(2, "Остальные категории");

    private final int id;

    private final String title;

    AdvertisementCategory(int id, String title) {
        this.id = id;
        this.title = title;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }
}
