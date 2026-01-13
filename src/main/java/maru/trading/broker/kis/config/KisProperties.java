package maru.trading.broker.kis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for KIS broker integration.
 * Maps to trading.broker.kis.* properties in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "trading.broker.kis")
public class KisProperties {

    private EnvironmentConfig paper = new EnvironmentConfig();
    private EnvironmentConfig live = new EnvironmentConfig();
    private TokenConfig token = new TokenConfig();
    private WebSocketConfig ws = new WebSocketConfig();

    public EnvironmentConfig getPaper() {
        return paper;
    }

    public void setPaper(EnvironmentConfig paper) {
        this.paper = paper;
    }

    public EnvironmentConfig getLive() {
        return live;
    }

    public void setLive(EnvironmentConfig live) {
        this.live = live;
    }

    public TokenConfig getToken() {
        return token;
    }

    public void setToken(TokenConfig token) {
        this.token = token;
    }

    public WebSocketConfig getWs() {
        return ws;
    }

    public void setWs(WebSocketConfig ws) {
        this.ws = ws;
    }

    /**
     * Environment-specific configuration (paper or live).
     */
    public static class EnvironmentConfig {
        private String baseUrl;
        private String wsUrl;
        private String appKey;
        private String appSecret;
        private String accountNo;
        private String accountProduct = "01";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getWsUrl() {
            return wsUrl;
        }

        public void setWsUrl(String wsUrl) {
            this.wsUrl = wsUrl;
        }

        public String getAppKey() {
            return appKey;
        }

        public void setAppKey(String appKey) {
            this.appKey = appKey;
        }

        public String getAppSecret() {
            return appSecret;
        }

        public void setAppSecret(String appSecret) {
            this.appSecret = appSecret;
        }

        public String getAccountNo() {
            return accountNo;
        }

        public void setAccountNo(String accountNo) {
            this.accountNo = accountNo;
        }

        public String getAccountProduct() {
            return accountProduct;
        }

        public void setAccountProduct(String accountProduct) {
            this.accountProduct = accountProduct;
        }
    }

    /**
     * Token configuration.
     */
    public static class TokenConfig {
        private int refreshBeforeMinutes = 30;

        public int getRefreshBeforeMinutes() {
            return refreshBeforeMinutes;
        }

        public void setRefreshBeforeMinutes(int refreshBeforeMinutes) {
            this.refreshBeforeMinutes = refreshBeforeMinutes;
        }
    }

    /**
     * WebSocket configuration.
     */
    public static class WebSocketConfig {
        private int reconnectDelayMs = 5000;
        private int maxReconnectAttempts = 10;

        public int getReconnectDelayMs() {
            return reconnectDelayMs;
        }

        public void setReconnectDelayMs(int reconnectDelayMs) {
            this.reconnectDelayMs = reconnectDelayMs;
        }

        public int getMaxReconnectAttempts() {
            return maxReconnectAttempts;
        }

        public void setMaxReconnectAttempts(int maxReconnectAttempts) {
            this.maxReconnectAttempts = maxReconnectAttempts;
        }
    }
}
