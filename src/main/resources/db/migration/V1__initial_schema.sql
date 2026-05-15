-- ── Users ──────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS users (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    email          VARCHAR(150) NOT NULL UNIQUE,
    password_hash  VARCHAR(255) NOT NULL,
    name           VARCHAR(100) NOT NULL,
    initials       VARCHAR(5),
    role           VARCHAR(20)  NOT NULL,
    phone          VARCHAR(30),
    location       VARCHAR(200),
    status         VARCHAR(20)  NOT NULL DEFAULT 'active',
    specialty      VARCHAR(100),
    department     VARCHAR(100),
    join_date      DATE,
    last_login     DATETIME(6)
);

-- ── Work Orders ────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS work_orders (
    id                    BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id              VARCHAR(20),
    type                  VARCHAR(20) NOT NULL,
    status                VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    client                VARCHAR(150) NOT NULL,
    location              VARCHAR(200),
    city                  VARCHAR(100),
    scheduled_date        DATE,
    scheduled_time        VARCHAR(20),
    technician_id         BIGINT,
    technician_name       VARCHAR(100),
    technician_initials   VARCHAR(5),
    priority              VARCHAR(30),
    contact_name          VARCHAR(100),
    contact_role          VARCHAR(100),
    contact_phone         VARCHAR(30),
    contact_email         VARCHAR(150),
    access_instructions   TEXT,
    expected_sensors      INT,
    asset_id              VARCHAR(50),
    technician_notes      TEXT,
    CONSTRAINT fk_wo_technician FOREIGN KEY (technician_id) REFERENCES users(id) ON DELETE SET NULL
);

-- ── Work Order Required Tools (ElementCollection) ──────────────────────────────
CREATE TABLE IF NOT EXISTS work_order_tools (
    work_order_id  BIGINT NOT NULL,
    tool           VARCHAR(255),
    CONSTRAINT fk_tools_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE
);

-- ── Sensors ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS sensors (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    sensor_id      VARCHAR(20),
    location       VARCHAR(200),
    status         VARCHAR(20) DEFAULT 'ok',
    work_order_id  BIGINT NOT NULL,
    CONSTRAINT fk_sensor_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE
);

-- ── Activity Log Entries ───────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS activity_log_entries (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    event          VARCHAR(200) NOT NULL,
    log_time       VARCHAR(50),
    work_order_id  BIGINT NOT NULL,
    CONSTRAINT fk_log_wo FOREIGN KEY (work_order_id) REFERENCES work_orders(id) ON DELETE CASCADE
);

-- ── Alerts ────────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS alerts (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    type           VARCHAR(20) NOT NULL,
    icon           VARCHAR(50),
    title          VARCHAR(200) NOT NULL,
    description    TEXT,
    relative_time  VARCHAR(100),
    created_at     DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);

-- ── History ───────────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS history (
    id                  BIGINT AUTO_INCREMENT PRIMARY KEY,
    order_id            VARCHAR(30),
    completion_date     DATE,
    completion_time     VARCHAR(20),
    client              VARCHAR(150),
    site                VARCHAR(200),
    service_type        VARCHAR(20),
    technician          VARCHAR(100),
    technician_initials VARCHAR(5),
    status              VARCHAR(20) NOT NULL DEFAULT 'completed',
    technician_id       BIGINT
);

-- ── Radiation Readings ────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS radiation_readings (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    reading_date  DATE NOT NULL,
    value         DOUBLE NOT NULL
);
