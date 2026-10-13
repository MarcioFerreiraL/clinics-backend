CREATE TABLE exam_types (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(150) NOT NULL,
    slug        VARCHAR(150) NOT NULL,
    category    VARCHAR(20)  NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_exam_types_name UNIQUE (name),
    CONSTRAINT uq_exam_types_slug UNIQUE (slug),
    CONSTRAINT ck_exam_types_category CHECK (category IN ('SANGUE', 'RAIO_X', 'CONSULTAS', 'CARDIO', 'ULTRASSOM'))
);

CREATE TABLE exams (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    clinic_id       UUID           NOT NULL REFERENCES clinics (id) ON DELETE CASCADE,
    exam_type_id    UUID           NOT NULL REFERENCES exam_types (id),
    price           NUMERIC(10, 2) NOT NULL,
    preparation     VARCHAR(255),
    payment_methods VARCHAR(255),
    available       BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    CONSTRAINT uq_exams_clinic_type UNIQUE (clinic_id, exam_type_id),
    CONSTRAINT ck_exams_price CHECK (price >= 0)
);

CREATE INDEX idx_exams_type_price ON exams (exam_type_id, price) WHERE available;
CREATE INDEX idx_exams_clinic ON exams (clinic_id);
