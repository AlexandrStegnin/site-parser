package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum City {

    MOSCOW(1, "moskva", "Москва"),
    TYUMEN(2, "tyumen", "Тюмень"),
    EKB(3, "ekaterinburg", "Екатеринбург");

    private final int id;

    private final String title;

    private final String description;

    City(int id, String title, String description) {
        this.id = id;
        this.title = title;
        this.description = description;
    }

    public int getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

}
