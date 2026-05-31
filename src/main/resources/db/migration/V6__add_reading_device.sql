ALTER TABLE radiation_readings ADD COLUMN device_id BIGINT NULL;
ALTER TABLE radiation_readings ADD CONSTRAINT fk_reading_device
    FOREIGN KEY (device_id) REFERENCES devices(id) ON DELETE SET NULL;
