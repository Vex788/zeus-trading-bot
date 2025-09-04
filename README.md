# Cryptocurrency Auto-Trading Bot

An AI-powered cryptocurrency trading bot with neural network forecasting and KuCoin API integration. Features both production and shadow modes for risk-free strategy testing.

## 🚀 Features

- **AI-Powered Trading**: Neural network-based price prediction with enhanced learning formula
- **Dual Mode Operation**: 
  - **Shadow Mode**: Risk-free virtual trading with $100 starting balance
  - **Production Mode**: Real trading with actual funds
- **KuCoin Integration**: Full API integration for real-time trading
- **Advanced Risk Management**: Stop-loss, take-profit, and position sizing controls
- **Real-Time Dashboard**: Web-based UI with live updates via WebSocket
- **Comprehensive Analytics**: Performance tracking and ML model metrics
- **Security**: Encrypted API key storage and audit logging
- **Technical Indicators**: RSI, MACD, Bollinger Bands integration

## 🏗️ Architecture

```
crypto-trading-bot/
├── src/main/java/com/trading/
│   ├── api/                    # REST API controllers
│   ├── core/
│   │   ├── engine/            # Trading engine and order management
│   │   ├── ml/                # Neural network and ML components
│   │   └── strategy/          # Trading strategies and risk management
│   ├── integration/
│   │   ├── kucoin/           # KuCoin API integration
│   │   └── websocket/        # Real-time updates
│   ├── domain/               # Entities and repositories
│   └── config/               # Security and configuration
├── src/main/resources/
│   ├── templates/            # Thymeleaf web templates
│   └── static/              # CSS, JS, and assets
└── docker/                  # Docker configuration files
```

## 🛠️ Technology Stack

- **Backend**: Java 17, Spring Boot 2.7.x
- **Database**: MySQL 8.0 (H2 for development)
- **ML Framework**: DeepLearning4J
- **Technical Analysis**: TA4J
- **Frontend**: Thymeleaf, Bootstrap 5, Chart.js
- **Security**: Spring Security, AES-256 encryption
- **Monitoring**: Spring Actuator, Prometheus, Grafana
- **Deployment**: Docker, Docker Compose

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker & Docker Compose (for containerized deployment)

### Local Development

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd crypto-trading-bot
   ```

2. **Configure application properties**
   ```bash
   cp src/main/resources/application.yml.example src/main/resources/application.yml
   # Edit the configuration file with your settings
   ```

3. **Run the application**
   ```bash
   mvn spring-boot:run
   ```

4. **Access the dashboard**
   - Open http://localhost:8080
   - Login with: `admin` / `admin123`

### Docker Deployment

1. **Set environment variables**
   ```bash
   export KUCOIN_API_KEY="your-api-key"
   export KUCOIN_SECRET_KEY="your-secret-key"
   export KUCOIN_PASSPHRASE="your-passphrase"
   export ENCRYPTION_KEY="your-encryption-key"
   ```

2. **Start with Docker Compose**
   ```bash
   docker-compose up -d
   ```

3. **Access services**
   - Trading Bot: http://localhost:8080
   - Grafana: http://localhost:3000 (admin/admin123)
   - Prometheus: http://localhost:9090

## 📊 Configuration

### Trading Bot Settings

```yaml
trading:
  bot:
    mode: SHADOW                    # SHADOW or PRODUCTION
    virtual-balance: 100.0          # Starting balance for shadow mode
    
    neural-network:
      learning-rate: 0.001
      epochs: 100
      batch-size: 32
      hidden-layers: [64, 32, 16]
    
    risk:
      max-position-size-percent: 10.0
      stop-loss-percent: 5.0
      take-profit-percent: 15.0
      max-daily-loss-percent: 20.0
    
    trading-pairs:
      - BTC-USDT
      - ETH-USDT
      - ADA-USDT
```

### KuCoin API Configuration

1. **Create KuCoin API credentials**
   - Go to KuCoin API Management
   - Create new API key with trading permissions
   - Note down API Key, Secret Key, and Passphrase

2. **Configure in application**
   - Use the web interface: Configuration → KuCoin API
   - Or set environment variables for Docker deployment

## 🧠 Neural Network

The bot uses an enhanced learning formula that adapts based on trading performance:

```
LearningRate = α * (1 + ROI) * AccuracyFactor * TimeDecay

Where:
- ROI = (CurrentBalance - InitialBalance) / InitialBalance
- AccuracyFactor = CorrectPredictions / TotalPredictions
- TimeDecay = 1 / (1 + β * TimeSinceLastTrade)
```

### Input Features (20 total)
- Price changes and momentum
- Volume ratios
- Technical indicators (RSI, MACD, Bollinger Bands)
- Moving averages
- Volatility measures
- Time-based features

### Output Predictions
- Price direction (up/down probability)
- Confidence level (0-1)
- Recommended position size (0-1)

## 🛡️ Security Features

- **Encrypted Storage**: API keys encrypted with AES-256
- **Audit Logging**: All trading operations logged
- **Rate Limiting**: API call throttling
- **Risk Management**: Multiple safety mechanisms
- **Authentication**: Spring Security integration
- **Input Validation**: Comprehensive parameter validation

## 📈 Monitoring & Analytics

### Dashboard Features
- Real-time portfolio performance
- Trading history and statistics
- ML model accuracy tracking
- Risk metrics and alerts
- Market data visualization

### Available Metrics
- Total return and profit/loss
- Win rate and success metrics
- Sharpe ratio and risk-adjusted returns
- Maximum drawdown
- ML prediction accuracy
- API response times

## 🧪 Testing

### Run Tests
```bash
# Unit tests
mvn test

# Integration tests
mvn verify

# Test coverage report
mvn jacoco:report
```

### Test Configuration
- Uses H2 in-memory database
- Mock KuCoin API responses
- Isolated test environment

## 🔧 API Endpoints

### Trading Control
- `POST /api/trading/start` - Start trading bot
- `POST /api/trading/stop` - Stop trading bot
- `POST /api/trading/pause` - Pause trading
- `POST /api/trading/resume` - Resume trading
- `POST /api/trading/mode/switch` - Switch between modes

### Dashboard Data
- `GET /api/dashboard/overview` - Main dashboard metrics
- `GET /api/dashboard/portfolio` - Portfolio information
- `GET /api/dashboard/trading-history` - Trading history
- `GET /api/dashboard/ml/performance` - ML metrics

### Configuration
- `GET /api/config/current` - Current configuration
- `POST /api/config/update` - Update configuration
- `POST /api/config/kucoin/credentials` - Update API credentials

## 🚨 Risk Warnings

⚠️ **Important Disclaimers:**

1. **Financial Risk**: Cryptocurrency trading involves substantial risk of loss
2. **No Guarantees**: Past performance does not guarantee future results
3. **Use Shadow Mode**: Always test strategies in shadow mode first
4. **API Security**: Keep your API credentials secure and never share them
5. **Regulatory Compliance**: Ensure compliance with local regulations

## 📝 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🤝 Contributing

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

## 📞 Support

- **Documentation**: Check the `/docs` folder for detailed guides
- **Issues**: Report bugs via GitHub Issues
- **Discussions**: Use GitHub Discussions for questions

## 🔄 Roadmap

- [ ] Additional exchange integrations (Binance, Coinbase)
- [ ] Advanced ML models (LSTM, Transformer)
- [ ] Portfolio optimization algorithms
- [ ] Mobile app for monitoring
- [ ] Backtesting framework
- [ ] Social trading features

---

**Disclaimer**: This software is for educational and research purposes. Use at your own risk. The authors are not responsible for any financial losses incurred through the use of this software.
