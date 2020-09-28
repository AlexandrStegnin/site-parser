package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum City {

    MOSCOW(1, "moskva", "Москва", "(московская).+(москва)"),
    TYUMEN(2, "tyumen", "Тюмень", "(тюменская).+(тюмень)"),
    EKB(3, "ekaterinburg", "Екатеринбург", "(свердловская).+(екатеринбург)");

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
