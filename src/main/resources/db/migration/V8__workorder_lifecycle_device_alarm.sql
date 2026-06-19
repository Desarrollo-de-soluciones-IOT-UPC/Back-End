-- ─────────────────────────────────────────────────────────────────────────────
-- V8: Work Order lifecycle (completion timestamp, cancellation reason, client FK),
--     evidence images, maintenance actions, device status transitions and
--     alarm targeting + alarm detail fields.
-- ─────────────────────────────────────────────────────────────────────────────

-- ── Work Orders: completion timestamp, cancellation reason, client FK ─────────
ALTER TABLE work_orders ADD COLUMN completed_at         DATETIME(6) NULL;
ALTER TABLE work_orders ADD COLUMN cancellation_reason  TEXT NULL;
ALTER TABLE work_orders ADD COLUMN client_user_id       BIGINT NULL;
ALTER TABLE work_orders ADD CONSTRAINT fk_wo_client_user
    FOREIGN KEY (client_user_id) REFERENCES users(id) ON DELETE SET NULL;

-- ── Work Order evidence (base64 data-URL images) ──────────────────────────────
CREATE TABLE IF NOT EXISTS work_order_evidence (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id  BIGINT NOT NULL,
    image          LONGTEXT NOT NULL,
    uploaded_at    DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_evidence_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE
);

-- ── Maintenance actions performed during a work order ─────────────────────────
CREATE TABLE IF NOT EXISTS maintenance_actions (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_id  BIGINT NOT NULL,
    device_id      BIGINT NULL,
    device_name    VARCHAR(150),
    action         VARCHAR(200) NOT NULL,
    description    TEXT,
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_maint_wo     FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE,
    CONSTRAINT fk_maint_device FOREIGN KEY (device_id)     REFERENCES devices(id)     ON DELETE SET NULL
);

-- ── Alarms: targeting + detail fields ─────────────────────────────────────────
ALTER TABLE alerts ADD COLUMN recipient_type VARCHAR(30)  NULL;
ALTER TABLE alerts ADD COLUMN client_name    VARCHAR(300) NULL;
ALTER TABLE alerts ADD COLUMN sensor         VARCHAR(100) NULL;

-- Specific-client recipients for an alarm (ElementCollection of client ids)
CREATE TABLE IF NOT EXISTS alert_recipient_clients (
    alert_id   BIGINT NOT NULL,
    client_id  BIGINT NOT NULL,
    CONSTRAINT fk_arc_alert FOREIGN KEY (alert_id) REFERENCES alerts(id) ON DELETE CASCADE
);

-- ── Normalize existing device statuses to the new vocabulary ──────────────────
--    active | in-maintenance | requires-maintenance | collecting | inactive
UPDATE devices SET status = 'in-maintenance' WHERE status = 'maintenance';
