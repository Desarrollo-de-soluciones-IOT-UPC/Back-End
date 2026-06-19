-- ─────────────────────────────────────────────────────────────────────────────
-- V10: Link history rows to their work order (for the detail view) and persist
--      the full client (company/individual) data captured in the edit form.
-- ─────────────────────────────────────────────────────────────────────────────

-- History → WorkOrder reference (full detail is fetched from the live work order).
ALTER TABLE history ADD COLUMN work_order_id BIGINT NULL;

-- Client (company / individual) fields persisted from the admin edit form.
ALTER TABLE users ADD COLUMN client_type   VARCHAR(20)  NULL;   -- company | individual
ALTER TABLE users ADD COLUMN tax_id        VARCHAR(50)  NULL;   -- RUC (company) / Doc ID (individual)
ALTER TABLE users ADD COLUMN industry      VARCHAR(50)  NULL;
ALTER TABLE users ADD COLUMN country       VARCHAR(100) NULL;
ALTER TABLE users ADD COLUMN contact_name  VARCHAR(150) NULL;
ALTER TABLE users ADD COLUMN contact_email VARCHAR(150) NULL;
ALTER TABLE users ADD COLUMN contact_phone VARCHAR(30)  NULL;

-- Existing seeded clients are companies.
UPDATE users SET client_type = 'company' WHERE role = 'CLIENT' AND client_type IS NULL;
