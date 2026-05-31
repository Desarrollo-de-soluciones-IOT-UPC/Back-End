ALTER TABLE devices ADD COLUMN client_id BIGINT NULL;
ALTER TABLE devices ADD CONSTRAINT fk_device_client
    FOREIGN KEY (client_id) REFERENCES users(id) ON DELETE SET NULL;
