CREATE TABLE adv_link
(
    id     INT8 PRIMARY KEY,
    name   VARCHAR(4000) NOT NULL,
    parsed BOOL DEFAULT FALSE
)
