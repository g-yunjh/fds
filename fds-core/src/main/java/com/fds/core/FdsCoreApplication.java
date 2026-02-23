package com.fds.core;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(com.fds.core.collector.BinanceWebSocketProperties.class)
public class FdsCoreApplication {

    public static void main(String[] args) {
        SpringApplication.run(FdsCoreApplication.class, args);
    }

}
