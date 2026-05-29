CREATE TABLE IF NOT EXISTS entry_point_event_type (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS entry_point_event (
    id BIGSERIAL PRIMARY KEY,
    event_type_id BIGINT NOT NULL REFERENCES entry_point_event_type(id),
    event_date TIMESTAMPTZ(6) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_event_type_date
    ON entry_point_event (event_type_id, event_date);

INSERT INTO entry_point_event_type (name) VALUES
    ('empty dehumidifier black'),
    ('empty dehumidifier white')
ON CONFLICT (name) DO NOTHING;
