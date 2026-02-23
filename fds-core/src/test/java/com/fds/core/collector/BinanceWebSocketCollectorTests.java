package com.fds.core.collector;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BinanceWebSocketCollectorTests {

    @Autowired
    private BinanceWebSocketProperties properties;

    @Autowired(required = false)
    private BinanceWebSocketCollector collector;

    @Test
    void propertiesHaveDefaults() {
        assertThat(properties.getUrl()).isNotBlank();
        assertThat(properties.getSymbols()).isNotEmpty();
        assertThat(properties.getTopic()).isNotBlank();
    }

    @Test
    void collectorBeanIsPresent() {
        assertThat(collector).isNotNull();
    }
}
