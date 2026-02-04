-- SQL script to remove unique constraint from bitcoin_address
-- Run this in PostgreSQL database: pspCrypto_app

-- Drop the unique constraint
ALTER TABLE transactions DROP CONSTRAINT IF EXISTS ukbbfad1prrexyra954s36dcd6b;

-- Verify constraint is removed
SELECT conname, contype 
FROM pg_constraint 
WHERE conrelid = 'transactions'::regclass 
AND conname LIKE '%bitcoin%';
