package com.ddkolesnik.siteparser.utils;

/**
 * @author Alexandr Stegnin
 */

public enum City {

    MOSCOW(1, "moskva", "Москва", "(московская)", "20000000", "400000"),
    TYUMEN(2, "tyumen", "Тюмень", "(тюменская)", "10000000", "100000"),
    EKB(3, "ekaterinburg", "Екатеринбург", "(свердловская)", "5000000", "50000");

    private final int id;

    private final String title;

    private final String description;

    private final String pattern;

    private final String salePrice;

    private final String rentPrice;

    City(int id, String title, String description, String pattern, String salePrice, String rentPrice) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.pattern = pattern;
        this.salePrice = salePrice;
        this.rentPrice = rentPrice;
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

    public String getSalePrice() {
        return salePrice;
    }

    public String getRentPrice() {
        return rentPrice;
    }
}
