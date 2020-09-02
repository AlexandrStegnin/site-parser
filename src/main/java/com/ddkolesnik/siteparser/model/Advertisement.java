package com.ddkolesnik.siteparser.model;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Alexandr Stegnin
 */

@Data
public class Advertisement {

    private String title;

    private String area;

    private BigDecimal price;

    private String address;

    private List<String> stations = new ArrayList<>();

    private String description;

    private String link;

    private String dateCreate;

    private String sellerName;

    private String sellerType;

    private String sellerOnAvito;

    private String sellerAdvComplete;

    private String sellerAdvActual;

    private String advType;

}
