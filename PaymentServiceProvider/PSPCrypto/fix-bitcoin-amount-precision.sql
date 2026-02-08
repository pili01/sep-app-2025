-- SQL script to fix bitcoin_amount column precision
-- Run this in PostgreSQL database: pspCrypto_app
-- This will allow storing Bitcoin amounts with up to 8 decimal places (standard for Bitcoin)

-- Alter the column to have precision 18, scale 8
ALTER TABLE transactions 
ALTER COLUMN bitcoin_amount TYPE NUMERIC(18,8);

-- Verify the change
SELECT 
    column_name, 
    data_type, 
    numeric_precision, 
    numeric_scale 
FROM information_schema.columns 
WHERE table_name = 'transactions' 
AND column_name = 'bitcoin_amount';
