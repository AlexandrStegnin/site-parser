package com.ddkolesnik.siteparser.model;

import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.persistence.*;
import java.math.BigDecimal;

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

    @Column(name = "adv_type")
    private String advType;

    @Column(name = "city")
    private String city;

}
