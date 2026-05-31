CREATE TABLE IF NOT EXISTS devices (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    name            VARCHAR(150) NOT NULL,
    type            VARCHAR(50)  NOT NULL,
    location        VARCHAR(200),
    status          VARCHAR(30)  NOT NULL DEFAULT 'active',
    serial_number   VARCHAR(100),
    install_date    DATE,
    created_at      DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
);
