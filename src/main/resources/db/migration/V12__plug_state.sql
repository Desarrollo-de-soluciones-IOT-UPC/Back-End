-- Relé "abre/cierra" (plug) end-to-end:
--  - radiation_readings.plug  → estado del relé REPORTADO por el dispositivo en cada lectura (ON/OFF)
--  - devices.desired_plug     → estado DESEADO por el usuario (comando mobile → backend → edge → dispositivo)
ALTER TABLE radiation_readings ADD COLUMN plug VARCHAR(8) NULL;
ALTER TABLE devices ADD COLUMN desired_plug VARCHAR(8) NULL;
