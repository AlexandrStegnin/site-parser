CREATE TABLE advertisement
(
    id                  INT8 PRIMARY KEY,
    title               VARCHAR(250) NOT NULL,
    area                VARCHAR(10)    DEFAULT NULL,
    price               DECIMAL(20, 2) DEFAULT NULL,
    address             VARCHAR(50)    DEFAULT NULL,
    stations            VARCHAR(50)    DEFAULT NULL,
    description         VARCHAR(8000)  DEFAULT NULL,
    link                VARCHAR(1000)  DEFAULT NULL,
    date_create         VARCHAR(30)    DEFAULT NULL,
    seller_name         VARCHAR(100)   DEFAULT NULL,
    seller_type         VARCHAR(20)    DEFAULT NULL,
    seller_on_avito     VARCHAR(30)    DEFAULT NULL,
    seller_adv_complete VARCHAR(20)    DEFAULT NULL,
    seller_adv_actual   VARCHAR(20)    DEFAULT NULL,
    adv_type            VARCHAR(10)    DEFAULT NULL
)
