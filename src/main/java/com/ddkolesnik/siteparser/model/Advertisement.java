package com.ddkolesnik.siteparser.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @author Alexandr Stegnin
 */

@Data
@Entity
@Table(name = "advertisement")
@EqualsAndHashCode(callSuper = true)
public class Advertisement extends AbstractEntity {

    @Column
    private String title;

    @Column
    private String area;

    @Column
    private BigDecimal price;

    @Column
    private String address;

    @Column
    private String stations;

    @Column
    private String description;

    @Column
    private String link;

    @Column(name = "date_create")
    private String dateCreate;

    @Column(name = "seller_name")
    private String sellerName;

    @Column(name = "seller_type")
    private String sellerType;

    @Column(name = "seller_on_avito")
    private String sellerOnAvito;

    @Column(name = "seller_adv_complete")
    private String sellerAdvComplete;

    @Column(name = "seller_adv_actual")
    private String sellerAdvActual;

    @Column(name = "adv_type")
    private String advType;

    @Column(name = "city")
    private String city;

    @Column(name = "publish_date")
    private LocalDate publishDate;

    @Column(name = "category")
    private String category;
}
