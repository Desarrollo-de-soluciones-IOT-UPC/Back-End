-- ─────────────────────────────────────────────────────────────────────────────
-- V11: Telemetría IoT — campos extra que envía el edge en cada lectura.
--      `level` y `message` vienen en el payload del edge; `recorded_at` es el
--      timestamp real de la lectura (antes solo había `reading_date`, sin hora).
-- ─────────────────────────────────────────────────────────────────────────────

ALTER TABLE radiation_readings ADD COLUMN level       VARCHAR(30)  NULL;  -- e.g. SEGURO | MODERADO | ALTO
ALTER TABLE radiation_readings ADD COLUMN message     VARCHAR(255) NULL;  -- texto descriptivo de la lectura
ALTER TABLE radiation_readings ADD COLUMN recorded_at DATETIME     NULL;  -- timestamp real de la medición

-- Backfill: las lecturas existentes solo tienen fecha; usamos esa fecha como timestamp.
UPDATE radiation_readings SET recorded_at = reading_date WHERE recorded_at IS NULL;
