ALTER TABLE advertisement ALTER COLUMN address TYPE varchar(255) USING address::varchar;
ALTER TABLE advertisement ALTER COLUMN stations TYPE varchar(255) USING stations::varchar;
ALTER TABLE advertisement ALTER COLUMN seller_name TYPE varchar(255) USING seller_name::varchar;
