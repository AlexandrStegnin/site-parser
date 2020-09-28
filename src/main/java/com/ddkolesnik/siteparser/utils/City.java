package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum City {

    MOSCOW(1, "moskva", "Москва", "(московская)"),
    TYUMEN(2, "tyumen", "Тюмень", "(тюменская)"),
    EKB(3, "ekaterinburg", "Екатеринбург", "(свердловская)");

    private final int id;

    private final String title;

    private final String description;

    private final String pattern;

    City(int id, String title, String description, String pattern) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.pattern = pattern;
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

    public String getPattern() {
        return pattern;
    }
}
