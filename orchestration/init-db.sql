CREATE DATABASE saga_db;
CREATE DATABASE order_db;
CREATE DATABASE payment_db;
CREATE DATABASE stock_db;

-- Connect to payment_db and seed customer balances
\c payment_db;

CREATE TABLE IF NOT EXISTS balances (
    user_id VARCHAR(50) PRIMARY KEY,
    balance DOUBLE PRECISION NOT NULL
);

INSERT INTO balances (user_id, balance) VALUES ('Alex', 150.00) ON CONFLICT (user_id) DO NOTHING;
INSERT INTO balances (user_id, balance) VALUES ('PoorBob', 15.00) ON CONFLICT (user_id) DO NOTHING;

-- Connect to stock_db and seed inventory
\c stock_db;

CREATE TABLE IF NOT EXISTS inventory (
    item_id VARCHAR(50) PRIMARY KEY,
    quantity INTEGER NOT NULL
);

INSERT INTO inventory (item_id, quantity) VALUES ('JavaBook', 5) ON CONFLICT (item_id) DO NOTHING;
INSERT INTO inventory (item_id, quantity) VALUES ('RareVinyl', 0) ON CONFLICT (item_id) DO NOTHING;
