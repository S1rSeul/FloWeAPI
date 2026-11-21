ALTER TYPE expense_category ADD VALUE IF NOT EXISTS 'SAVINGS';

CREATE TABLE savings_accounts (
    id              UUID PRIMARY KEY       DEFAULT gen_random_uuid(),
    couple_id       UUID           NOT NULL REFERENCES couples (id) ON DELETE CASCADE,
    name            VARCHAR(100)   NOT NULL,
    target_amount   NUMERIC(12, 2),
    current_balance NUMERIC(12, 2) NOT NULL DEFAULT 0 CHECK (current_balance >= 0),
    currency        currency_code  NOT NULL DEFAULT 'RUB',
    created_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),

    CONSTRAINT unique_couple_savings_name UNIQUE (couple_id, name)
);

CREATE INDEX idx_savings_couple_id ON savings_accounts (couple_id);