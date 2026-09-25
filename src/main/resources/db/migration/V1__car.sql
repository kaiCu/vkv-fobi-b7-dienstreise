-- The three pool cars (FR-002, FR-003). Rows are reconciled from configuration at startup.
CREATE TABLE car (
    id            smallint    PRIMARY KEY CHECK (id BETWEEN 1 AND 3),
    display_name  varchar(80) NOT NULL CHECK (btrim(display_name) <> ''),
    license_plate varchar(15) NOT NULL UNIQUE CHECK (btrim(license_plate) <> '')
);
