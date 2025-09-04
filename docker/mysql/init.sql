-- Initialize MySQL database for Crypto Trading Bot

-- Create database if not exists
CREATE DATABASE IF NOT EXISTS trading_bot;

-- Use the trading_bot database
USE trading_bot;

-- Create bot_config table
CREATE TABLE IF NOT EXISTS bot_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    mode VARCHAR(20) NOT NULL,
    api_key TEXT,
    secret_key TEXT,
    virtual_balance DECIMAL(20,8),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Create orders table
CREATE TABLE IF NOT EXISTS orders (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id VARCHAR(100) UNIQUE,
    trading_pair VARCHAR(20),
    side VARCHAR(10),
    price DECIMAL(20,8),
    amount DECIMAL(20,8),
    status VARCHAR(20),
    is_virtual BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    filled_at TIMESTAMP NULL,
    filled_amount DECIMAL(20,8) DEFAULT 0
);

-- Create portfolio table
CREATE TABLE IF NOT EXISTS portfolio (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    currency VARCHAR(10),
    balance DECIMAL(20,8),
    available_balance DECIMAL(20,8),
    locked_balance DECIMAL(20,8) DEFAULT 0,
    is_virtual BOOLEAN DEFAULT FALSE,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Create ml_predictions table
CREATE TABLE IF NOT EXISTS ml_predictions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    prediction_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trading_pair VARCHAR(20),
    predicted_price DECIMAL(20,8),
    actual_price DECIMAL(20,8),
    accuracy_score DECIMAL(5,4),
    reward_coefficient DECIMAL(10,8),
    direction VARCHAR(10),
    confidence_level DECIMAL(5,4)
);

-- Create trading_history table
CREATE TABLE IF NOT EXISTS trading_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    trade_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    trading_pair VARCHAR(20),
    action VARCHAR(10),
    amount DECIMAL(20,8),
    price DECIMAL(20,8),
    profit_loss DECIMAL(20,8),
    balance_after DECIMAL(20,8),
    is_virtual BOOLEAN DEFAULT FALSE,
    strategy_used VARCHAR(50),
    ml_confidence DECIMAL(5,4)
);

-- Create indexes for better performance
CREATE INDEX idx_orders_trading_pair ON orders(trading_pair);
CREATE INDEX idx_orders_status ON orders(status);
CREATE INDEX idx_orders_created_at ON orders(created_at);
CREATE INDEX idx_trading_history_trade_time ON trading_history(trade_time);
CREATE INDEX idx_trading_history_trading_pair ON trading_history(trading_pair);
CREATE INDEX idx_ml_predictions_prediction_time ON ml_predictions(prediction_time);
CREATE INDEX idx_ml_predictions_trading_pair ON ml_predictions(trading_pair);
CREATE INDEX idx_portfolio_currency ON portfolio(currency);

-- Insert initial configuration
INSERT INTO bot_config (mode, virtual_balance) 
VALUES ('SHADOW', 100.00) 
ON DUPLICATE KEY UPDATE mode = VALUES(mode);

-- Insert initial virtual portfolio
INSERT INTO portfolio (currency, balance, available_balance, is_virtual) 
VALUES ('USDT', 100.00, 100.00, TRUE) 
ON DUPLICATE KEY UPDATE balance = VALUES(balance), available_balance = VALUES(available_balance);

-- Grant privileges to trading_user
GRANT ALL PRIVILEGES ON trading_bot.* TO 'trading_user'@'%';
FLUSH PRIVILEGES;