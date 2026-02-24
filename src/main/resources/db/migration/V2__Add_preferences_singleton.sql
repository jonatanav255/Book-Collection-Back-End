-- Enforce single preferences row by adding a unique constraint on a constant column
-- This prevents multiple preference rows from being created
ALTER TABLE preferences ADD COLUMN singleton BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE preferences ADD CONSTRAINT uq_preferences_singleton UNIQUE (singleton);
ALTER TABLE preferences ADD CONSTRAINT chk_preferences_singleton CHECK (singleton = TRUE);
