CREATE TABLE IF NOT EXISTS arduino (
    id BIGSERIAL PRIMARY KEY,
    host_name VARCHAR(255) UNIQUE,
    is_active BOOLEAN NOT NULL,
    creation_date TIMESTAMPTZ(6) DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_hostname ON arduino (host_name);

CREATE TABLE IF NOT EXISTS sensor_data (
    id BIGSERIAL PRIMARY KEY,
    machine_name VARCHAR(255),
    creation_date TIMESTAMPTZ(6),
    temperature VARCHAR(255),
    humidity VARCHAR(255)
);

CREATE INDEX IF NOT EXISTS idx_machine_date
    ON sensor_data (machine_name, creation_date);
