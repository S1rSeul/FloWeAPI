CREATE TABLE events (
    id            UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    couple_id     UUID         NOT NULL REFERENCES couples(id) ON DELETE CASCADE,
    created_by_id UUID         NOT NULL REFERENCES users(id),
    title         VARCHAR(100) NOT NULL,
    description   TEXT,
    starts_at     TIMESTAMPTZ  NOT NULL,
    ends_at       TIMESTAMPTZ,
    is_shared     BOOLEAN      NOT NULL DEFAULT true,
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE INDEX idx_events_couple_id_starts_at
    ON events (couple_id, starts_at);

CREATE INDEX idx_events_created_by_id
    ON events (created_by_id);