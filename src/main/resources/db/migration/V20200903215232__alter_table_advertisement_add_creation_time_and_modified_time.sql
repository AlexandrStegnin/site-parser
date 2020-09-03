ALTER TABLE advertisement ADD creation_time TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE advertisement ADD modified_time TIMESTAMP DEFAULT NULL;
