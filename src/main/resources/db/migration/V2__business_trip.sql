CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE business_trip (
    id            uuid         PRIMARY KEY,
    owner_subject varchar(255) NOT NULL,
    car_id        smallint     NOT NULL REFERENCES car (id) ON DELETE RESTRICT,
    start_at      timestamptz  NOT NULL,
    end_at        timestamptz  NOT NULL,
    destination   varchar(200) NOT NULL,
    created_at    timestamptz  NOT NULL DEFAULT now(),
    updated_at    timestamptz  NOT NULL,
    version       integer      NOT NULL DEFAULT 0,
    CONSTRAINT business_trip_end_after_start CHECK (end_at > start_at),
    CONSTRAINT business_trip_destination_len CHECK (char_length(btrim(destination)) BETWEEN 1 AND 200),
    -- FR-017/FR-018: no overlapping periods per car; back-to-back trips are allowed
    CONSTRAINT business_trip_no_overlap
        EXCLUDE USING gist (car_id WITH =, tstzrange(start_at, end_at, '[)') WITH &&)
);

CREATE INDEX business_trip_owner_start_idx ON business_trip (owner_subject, start_at, created_at, id);
CREATE INDEX business_trip_end_idx ON business_trip (end_at);
