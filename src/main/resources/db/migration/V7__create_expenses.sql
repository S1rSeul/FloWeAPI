CREATE TYPE expense_category AS ENUM (
    'FOOD',
    'TRANSPORT',
    'HOUSING',
    'ENTERTAINMENT',
    'HEALTH',
    'SHOPPING',
    'OTHER'
);

CREATE TYPE currency_code AS ENUM (
    'RUB',
    'USD',
    'EUR'
);

CREATE TABLE expenses (
    id                UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    couple_id         UUID           NOT NULL REFERENCES couples (id) ON DELETE CASCADE,
    created_by_id     UUID           NOT NULL REFERENCES users (id),

    amount            NUMERIC(12, 2) NOT NULL CHECK (amount > 0),
    user1_paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (user1_paid_amount >= 0),
    user2_paid_amount NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (user2_paid_amount >= 0),

    currency          currency_code  NOT NULL DEFAULT 'RUB',
    category          expense_category NOT NULL,
    spent_at          TIMESTAMPTZ    NOT NULL,
    description       TEXT,
    created_at        TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ    NOT NULL DEFAULT now()
);

CREATE INDEX idx_expenses_couple_id
    ON expenses (couple_id);

CREATE INDEX idx_expenses_couple_spent_at
    ON expenses (couple_id, spent_at DESC);

CREATE INDEX idx_expenses_couple_category
    ON expenses (couple_id, category);