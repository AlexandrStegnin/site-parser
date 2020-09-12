ALTER TABLE advertisement ALTER COLUMN seller_type TYPE varchar(40) USING area::varchar;
