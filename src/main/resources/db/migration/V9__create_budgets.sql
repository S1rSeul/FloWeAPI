CREATE TYPE budget_period AS ENUM ('MONTHLY');

ALTER TABLE expenses
    ADD COLUMN savings_account_id UUID REFERENCES savings_accounts (id) ON DELETE SET NULL;

CREATE INDEX idx_expenses_savings_account
    ON expenses (savings_account_id, spent_at)
    WHERE savings_account_id IS NOT NULL;

CREATE TABLE budgets (
        id                 UUID PRIMARY KEY        DEFAULT gen_random_uuid(),
        couple_id          UUID           NOT NULL REFERENCES couples (id) ON DELETE CASCADE,
        category           expense_category,
        savings_account_id UUID REFERENCES savings_accounts (id) ON DELETE CASCADE,
        limit_amount       NUMERIC(12, 2) NOT NULL CHECK (limit_amount > 0),
        currency           currency_code  NOT NULL,
        period             budget_period  NOT NULL DEFAULT 'MONTHLY',
        created_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),
        updated_at         TIMESTAMPTZ    NOT NULL DEFAULT now(),

        CONSTRAINT budgets_exactly_one_target CHECK (
            (category IS NOT NULL AND savings_account_id IS NULL)
                OR (category IS NULL AND savings_account_id IS NOT NULL)
        )
);

CREATE INDEX idx_budgets_couple_id ON budgets (couple_id);

CREATE UNIQUE INDEX uq_budgets_category
    ON budgets (couple_id, category, currency, period)
    WHERE category IS NOT NULL;

CREATE UNIQUE INDEX uq_budgets_savings_account
    ON budgets (savings_account_id, period)
    WHERE savings_account_id IS NOT NULL;