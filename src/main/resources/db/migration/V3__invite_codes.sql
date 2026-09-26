CREATE TABLE invite_codes
(
    id          UUID PRIMARY KEY     DEFAULT gen_random_uuid(),
    couple_id   UUID        NOT NULL REFERENCES couples (id) ON DELETE CASCADE,
    invite_code VARCHAR(10) NOT NULL UNIQUE,
);
CREATE INDEX idx_invite_codes_couple_id
    ON invite_codes (couple_id);