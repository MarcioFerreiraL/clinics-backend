ALTER TABLE exams ADD COLUMN description TEXT;

CREATE UNIQUE INDEX uq_clinics_owner_user
    ON clinics (owner_user_id)
    WHERE owner_user_id IS NOT NULL;

CREATE TABLE exam_view_events (
    id         UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    exam_id    UUID        NOT NULL REFERENCES exams (id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_exam_view_events_exam_created
    ON exam_view_events (exam_id, created_at DESC);
