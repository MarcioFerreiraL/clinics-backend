CREATE TABLE clinics (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    owner_user_id   UUID REFERENCES users (id) ON DELETE SET NULL,
    name            VARCHAR(150)  NOT NULL,
    description     TEXT,
    address         VARCHAR(255)  NOT NULL,
    neighborhood    VARCHAR(100),
    city            VARCHAR(100)  NOT NULL DEFAULT 'Surubim',
    state           CHAR(2)       NOT NULL DEFAULT 'PE',
    latitude        NUMERIC(9, 6),
    longitude       NUMERIC(9, 6),
    phone_number    VARCHAR(20),
    whatsapp_number VARCHAR(15)   NOT NULL,
    logo_url        VARCHAR(500),
    rating          NUMERIC(2, 1) NOT NULL DEFAULT 0,
    review_count    INTEGER       NOT NULL DEFAULT 0,
    patient_count   INTEGER       NOT NULL DEFAULT 0,
    is_verified     BOOLEAN       NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT now(),
    CONSTRAINT ck_clinics_rating CHECK (rating BETWEEN 0 AND 5)
);

CREATE TABLE clinic_operating_hours (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id   UUID     NOT NULL REFERENCES clinics (id) ON DELETE CASCADE,
    day_of_week SMALLINT NOT NULL,
    opens_at    TIME     NOT NULL,
    closes_at   TIME     NOT NULL,
    CONSTRAINT uq_hours_clinic_day_open UNIQUE (clinic_id, day_of_week, opens_at),
    CONSTRAINT ck_hours_day CHECK (day_of_week BETWEEN 1 AND 7),
    CONSTRAINT ck_hours_interval CHECK (closes_at > opens_at)
);
