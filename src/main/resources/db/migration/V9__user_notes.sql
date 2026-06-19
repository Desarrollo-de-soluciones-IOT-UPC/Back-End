-- V9: free-text notes on users (persisted from the admin user edit forms).
ALTER TABLE users ADD COLUMN notes TEXT NULL;
