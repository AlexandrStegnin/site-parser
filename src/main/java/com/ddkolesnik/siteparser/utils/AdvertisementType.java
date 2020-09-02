package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum AdvertisementType {

    SALE(1, "Продажа"),
    RENT(2, "Аренда");

    private final int id;

    private final String title;

    AdvertisementType(int id, String title) {
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
