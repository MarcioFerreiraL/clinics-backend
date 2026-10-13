CREATE TABLE favorites (
    user_id      UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    exam_type_id UUID        NOT NULL REFERENCES exam_types (id) ON DELETE CASCADE,
    created_at   TIMESTAMPTZ NOT NULL DEFAULT now(),
    PRIMARY KEY (user_id, exam_type_id)
);

CREATE TABLE reviews (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id  UUID        NOT NULL REFERENCES clinics (id) ON DELETE CASCADE,
    user_id    UUID        NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    rating     SMALLINT    NOT NULL,
    comment    VARCHAR(1000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_reviews_user_clinic UNIQUE (user_id, clinic_id),
    CONSTRAINT ck_reviews_rating CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_clinic ON reviews (clinic_id, created_at DESC);

CREATE TABLE price_history (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id     UUID           NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    price       NUMERIC(10, 2) NOT NULL,
    recorded_at TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_price_history_price CHECK (price >= 0)
);

CREATE INDEX idx_price_history_exam ON price_history (exam_id, recorded_at DESC);

CREATE TABLE contact_events (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id           UUID           NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    user_id           UUID REFERENCES users (id) ON DELETE SET NULL,
    price_at_contact  NUMERIC(10, 2) NOT NULL,
    estimated_savings NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT ck_contact_savings CHECK (estimated_savings >= 0)
);

CREATE INDEX idx_contact_events_created ON contact_events (created_at DESC);
CREATE INDEX idx_contact_events_exam_created ON contact_events (exam_id, created_at DESC);
CREATE INDEX idx_contact_events_user ON contact_events (user_id);
