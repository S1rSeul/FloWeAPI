CREATE TYPE task_status AS ENUM ('todo', 'in_progress', 'done', 'closed');

CREATE TABLE tasks
(
    id                UUID PRIMARY KEY      DEFAULT gen_random_uuid(),
    couple_id         UUID         NOT NULL REFERENCES couples (id) ON DELETE CASCADE,
    title             VARCHAR(100) NOT NULL,
    description       TEXT,
    created_by_id     UUID         NOT NULL REFERENCES users (id),
    is_user1_assignee BOOLEAN      NOT NULL DEFAULT false,
    is_user2_assignee BOOLEAN      NOT NULL DEFAULT false,
    due_date          TIMESTAMPTZ,
    status            task_status  NOT NULL DEFAULT 'todo',
    completed_at      TIMESTAMPTZ,
    created_at        TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at        TIMESTAMPTZ  NOT NULL DEFAULT now()
);
CREATE INDEX idx_tasks_couple_id
    ON tasks (couple_id);

CREATE INDEX idx_tasks_created_by_id
    ON tasks (created_by_id);