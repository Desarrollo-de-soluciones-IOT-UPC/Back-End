ALTER TABLE radiation_readings ADD COLUMN latitude  DOUBLE NULL;
ALTER TABLE radiation_readings ADD COLUMN longitude DOUBLE NULL;
ALTER TABLE radiation_readings ADD COLUMN location  VARCHAR(200) NULL;
ALTER TABLE radiation_readings ADD COLUMN sensor_id VARCHAR(50)  NULL;
UPDATE radiation_readings SET latitude = 30.2672 + (id * 0.01), longitude = -97.7431 + (id * 0.01), location = CONCAT('Site ', id), sensor_id = CONCAT('#S-', 100 + id) WHERE latitude IS NULL;
