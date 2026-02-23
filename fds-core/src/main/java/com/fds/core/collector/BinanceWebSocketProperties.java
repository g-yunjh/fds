package com.fds.core.collector;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "binance.websocket")
public class BinanceWebSocketProperties {

    /**
     * Base URL for the Binance websocket endpoint.  Usually
     * {@code wss://stream.binance.com:9443/ws}.
     */
    private String url = "wss://stream.binance.com:9443/ws";

    /**
     * Comma-separated list of symbol streams to subscribe to.  Each entry should
     * look like {@code btcusdt@trade}.  When the collector starts it will
     * concatenate them with {@code /} and open a single websocket connection.
     */
    private List<String> symbols = new ArrayList<>();

    /**
     * Kafka topic where raw json messages will be forwarded.
     */
    private String topic = "binance-trades";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public List<String> getSymbols() {
        return symbols;
    }

    public void setSymbols(List<String> symbols) {
        this.symbols = symbols;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }
}
