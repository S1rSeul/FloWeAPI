CREATE TYPE couple_status AS ENUM ('pending', 'active');

CREATE TABLE couples
(
    id              UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    name            VARCHAR(100)  NOT NULL,
    status          couple_status NOT NULL DEFAULT 'pending',
    user1_id        UUID          NOT NULL UNIQUE REFERENCES users (id),
    user2_id        UUID UNIQUE REFERENCES users (id),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT now()
);