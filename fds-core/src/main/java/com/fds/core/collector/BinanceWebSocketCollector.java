package com.fds.core.collector;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.time.Duration;

import org.springframework.web.reactive.socket.client.ReactorNettyWebSocketClient;
import org.springframework.web.reactive.socket.WebSocketMessage;

/**
 * Lightweight component that opens a websocket connection to Binance and
 * forwards every received message to a Kafka topic.  The URL, symbol(s) and
 * topic are configurable via {@link BinanceWebSocketProperties}.
 *
 * <p>The implementation is intentionally very simple: it concatenates the
 * configured symbol streams, opens a single connection and logs/produces each
 * JSON payload as-is.  In a real application you would probably transform the
 * JSON into typed events and handle reconnect/backoff more carefully.
 */
@Component
public class BinanceWebSocketCollector {

    private static final Logger log = LoggerFactory.getLogger(BinanceWebSocketCollector.class);

    private final BinanceWebSocketProperties props;
    private final KafkaTemplate<String, String> kafkaTemplate; // may be null in tests or when Kafka is not configured
    private final ReactorNettyWebSocketClient client;

    public BinanceWebSocketCollector(BinanceWebSocketProperties props,
                                     org.springframework.beans.factory.ObjectProvider<KafkaTemplate<String, String>> kafkaTemplateProvider) {
        this.props = props;
        // only bind if a template is available; some tests or environments may not
        // configure Kafka at all.
        this.kafkaTemplate = kafkaTemplateProvider.getIfAvailable();
        this.client = new ReactorNettyWebSocketClient();
    }

    @PostConstruct
    public void start() {
        if (props.getSymbols().isEmpty()) {
            log.warn("No symbols configured for Binance websocket collector; will not connect");
            return;
        }

        String path = String.join("/", props.getSymbols());
        String wsUrl = props.getUrl();
        URI uri = URI.create(wsUrl + "/" + path);

        log.info("opening websocket connection to Binance: {}", uri);

        client.execute(uri, session ->
                session.receive()
                        .map(WebSocketMessage::getPayloadAsText)
                        .doOnNext(this::handleMessage)
                        .then()
        )
        .retryWhen(Retry.backoff(Long.MAX_VALUE, Duration.ofSeconds(5))
                .doBeforeRetry(signal -> log.warn("reconnecting to Binance websocket (attempt {})", signal.totalRetries()))
        )
        .subscribe();
    }

    private void handleMessage(String message) {
        // forward the raw payload to Kafka; using the first field of the JSON
        // (usually "e" for event type) would be more appropriate as the key.
        log.debug("received {} bytes", message.length());
        if (kafkaTemplate != null) {
            kafkaTemplate.send(props.getTopic(), message);
        } else {
            log.warn("no KafkaTemplate available, message will not be sent");
        }
    }
}
