ALTER TABLE adv_link ADD creation_time TIMESTAMP NOT NULL DEFAULT NOW();
ALTER TABLE adv_link ADD modified_time TIMESTAMP DEFAULT NULL;
