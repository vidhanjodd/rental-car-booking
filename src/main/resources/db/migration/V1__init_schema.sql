
CREATE EXTENSION IF NOT EXISTS "pgcrypto";


CREATE TYPE user_role      AS ENUM ('ROLE_USER', 'ROLE_ADMIN');
CREATE TYPE car_status     AS ENUM ('AVAILABLE', 'BOOKED', 'MAINTENANCE', 'RETIRED');
CREATE TYPE car_category   AS ENUM ('ECONOMY', 'COMPACT', 'SEDAN', 'SUV', 'LUXURY', 'VAN', 'TRUCK');
CREATE TYPE booking_status AS ENUM ('PENDING', 'CONFIRMED', 'CANCELLED', 'COMPLETED');


CREATE TABLE users (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    email        VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255) NOT NULL,
    first_name   VARCHAR(50)  NOT NULL,
    last_name    VARCHAR(50)  NOT NULL,
    phone        VARCHAR(20),
    role         VARCHAR(20)  NOT NULL DEFAULT 'ROLE_USER',
    enabled      BOOLEAN      NOT NULL DEFAULT TRUE,
    deleted      BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    created_by   VARCHAR(100),
    updated_by   VARCHAR(100)
);

CREATE INDEX idx_users_email    ON users (email)    WHERE deleted = FALSE;
CREATE INDEX idx_users_username ON users (username) WHERE deleted = FALSE;
CREATE INDEX idx_users_role     ON users (role)     WHERE deleted = FALSE;


CREATE TABLE cars (
    id               UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    brand            VARCHAR(50)    NOT NULL,
    model            VARCHAR(50)    NOT NULL,
    year             INTEGER        NOT NULL,
    license_plate    VARCHAR(20)    NOT NULL UNIQUE,
    category         VARCHAR(20)    NOT NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'AVAILABLE',
    daily_rate       NUMERIC(10,2)  NOT NULL,
    city             VARCHAR(100)   NOT NULL,
    seats            INTEGER        NOT NULL,
    transmission     VARCHAR(20),
    fuel_type        VARCHAR(20),
    description      TEXT,
    image_url        VARCHAR(500),
    deleted          BOOLEAN        NOT NULL DEFAULT FALSE,
    version          BIGINT         NOT NULL DEFAULT 0,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by       VARCHAR(100),
    updated_by       VARCHAR(100)
);

CREATE INDEX idx_cars_status         ON cars (status)       WHERE deleted = FALSE;
CREATE INDEX idx_cars_city_status    ON cars (city, status) WHERE deleted = FALSE;
CREATE INDEX idx_cars_category       ON cars (category)     WHERE deleted = FALSE;
CREATE INDEX idx_cars_license_plate  ON cars (license_plate);


CREATE TABLE bookings (
    id                    UUID           NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id               UUID           NOT NULL REFERENCES users(id),
    car_id                UUID           NOT NULL REFERENCES cars(id),
    start_date            DATE           NOT NULL,
    end_date              DATE           NOT NULL,
    status                VARCHAR(20)    NOT NULL DEFAULT 'PENDING',
    total_price           NUMERIC(10,2)  NOT NULL,
    daily_rate_snapshot   NUMERIC(10,2)  NOT NULL,
    pickup_location       VARCHAR(200),
    dropoff_location      VARCHAR(200),
    notes                 TEXT,
    cancellation_reason   TEXT,
    version               BIGINT         NOT NULL DEFAULT 0,
    created_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    updated_at            TIMESTAMPTZ    NOT NULL DEFAULT NOW(),
    created_by            VARCHAR(100),
    updated_by            VARCHAR(100),

    CONSTRAINT chk_booking_dates CHECK (end_date > start_date)
);

CREATE INDEX idx_bookings_user_id   ON bookings (user_id);
CREATE INDEX idx_bookings_car_id    ON bookings (car_id);
CREATE INDEX idx_bookings_status    ON bookings (status);
CREATE INDEX idx_bookings_dates     ON bookings (start_date, end_date);
CREATE INDEX idx_bookings_car_dates ON bookings (car_id, start_date, end_date);

CREATE UNIQUE INDEX idx_bookings_no_overlap
    ON bookings (car_id, start_date, end_date)
    WHERE status IN ('PENDING', 'CONFIRMED');


CREATE TABLE refresh_tokens (
    id          UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id     UUID         NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    revoked     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user  ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token) WHERE revoked = FALSE;


CREATE TABLE audit_logs (
    id           UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    entity_type  VARCHAR(50)  NOT NULL,
    entity_id    UUID         NOT NULL,
    action       VARCHAR(100) NOT NULL,
    old_value    TEXT,
    new_value    TEXT,
    actor        VARCHAR(100),
    ip_address   VARCHAR(45),
    details      TEXT,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_entity     ON audit_logs (entity_type, entity_id);
CREATE INDEX idx_audit_actor      ON audit_logs (actor);
CREATE INDEX idx_audit_created_at ON audit_logs (created_at DESC);


INSERT INTO users (id, username, email, password, first_name, last_name, role, created_by)
VALUES (
    gen_random_uuid(),
    'admin',
    'admin@rentalcar.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQyCH.YGFlSVWXA5rHzLhRBGq',
    'System',
    'Admin',
    'ROLE_ADMIN',
    'SYSTEM'
);

INSERT INTO cars (brand, model, year, license_plate, category, daily_rate, city, seats, transmission, fuel_type, created_by)
VALUES
    ('Toyota',    'Camry',        2022, 'TN01AB1234', 'SEDAN',   1500.00, 'Chennai',   5, 'AUTOMATIC', 'PETROL',   'SYSTEM'),
    ('Hyundai',   'Creta',        2023, 'TN02CD5678', 'SUV',     2000.00, 'Chennai',   5, 'AUTOMATIC', 'PETROL',   'SYSTEM'),
    ('Maruti',    'Swift',        2022, 'KA01EF9012', 'COMPACT', 900.00,  'Bengaluru', 5, 'MANUAL',    'PETROL',   'SYSTEM'),
    ('Tata',      'Nexon EV',     2023, 'KA02GH3456', 'SUV',     2500.00, 'Bengaluru', 5, 'AUTOMATIC', 'ELECTRIC', 'SYSTEM'),
    ('Honda',     'City',         2022, 'MH01IJ7890', 'SEDAN',   1800.00, 'Mumbai',    5, 'AUTOMATIC', 'PETROL',   'SYSTEM'),
    ('Mahindra',  'Thar',         2023, 'DL01KL2345', 'SUV',     3000.00, 'Delhi',     4, 'MANUAL',    'DIESEL',   'SYSTEM'),
    ('Kia',       'Seltos',       2023, 'DL02MN6789', 'SUV',     2200.00, 'Delhi',     5, 'AUTOMATIC', 'PETROL',   'SYSTEM'),
    ('BMW',       '3 Series',     2022, 'MH02OP1234', 'LUXURY',  5000.00, 'Mumbai',    5, 'AUTOMATIC', 'PETROL',   'SYSTEM'),
    ('Mercedes',  'GLC',          2023, 'KA03QR5678', 'LUXURY',  6000.00, 'Bengaluru', 5, 'AUTOMATIC', 'DIESEL',   'SYSTEM'),
    ('Innova',    'Crysta',       2022, 'TN03ST9012', 'VAN',     2800.00, 'Chennai',   7, 'AUTOMATIC', 'DIESEL',   'SYSTEM');
