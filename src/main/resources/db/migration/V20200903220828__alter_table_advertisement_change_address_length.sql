ALTER TABLE advertisement ALTER COLUMN address TYPE varchar(100) USING stations::varchar;
