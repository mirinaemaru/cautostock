package maru.trading.broker.kis.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for KIS broker integration.
 * Maps to kis.* properties in application.yml.
 */
@Configuration
@ConfigurationProperties(prefix = "kis")
public class KisProperties {

    private String paperBaseUrl = "https://openapivts.koreainvestment.com:29443"; // PAPER REST
    private String liveBaseUrl = "https://openapi.koreainvestment.com:9443"; // LIVE REST
    private String paperWsUrl = "ws://ops.koreainvestment.com:21000"; // PAPER WebSocket
    private String liveWsUrl = "ws://ops.koreainvestment.com:31000"; // LIVE WebSocket

    private String appKey;
    private String appSecret;

    private WebSocketConfig websocket = new WebSocketConfig();

    public String getPaperBaseUrl() {
        return paperBaseUrl;
    }

    public void setPaperBaseUrl(String paperBaseUrl) {
        this.paperBaseUrl = paperBaseUrl;
    }

    public String getLiveBaseUrl() {
        return liveBaseUrl;
    }

    public void setLiveBaseUrl(String liveBaseUrl) {
        this.liveBaseUrl = liveBaseUrl;
    }

    public String getPaperWsUrl() {
        return paperWsUrl;
    }

    public void setPaperWsUrl(String paperWsUrl) {
        this.paperWsUrl = paperWsUrl;
    }

    public String getLiveWsUrl() {
        return liveWsUrl;
    }

    public void setLiveWsUrl(String liveWsUrl) {
        this.liveWsUrl = liveWsUrl;
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

    public WebSocketConfig getWebsocket() {
        return websocket;
    }

    public void setWebsocket(WebSocketConfig websocket) {
        this.websocket = websocket;
    }

    public static class WebSocketConfig {
        private int reconnectDelayMs = 5000; // 5 seconds
        private int maxReconnectAttempts = 10;
        private int pingIntervalMs = 30000; // 30 seconds

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

        public int getPingIntervalMs() {
            return pingIntervalMs;
        }

        public void setPingIntervalMs(int pingIntervalMs) {
            this.pingIntervalMs = pingIntervalMs;
        }
    }
}
