ALTER TABLE advertisement ALTER COLUMN seller_adv_complete TYPE varchar(40) USING area::varchar;
ALTER TABLE advertisement ALTER COLUMN seller_adv_actual TYPE varchar(40) USING area::varchar;
