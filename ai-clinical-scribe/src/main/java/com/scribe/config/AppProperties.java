package com.scribe.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(Ai ai, Cors cors) {

    public record Ai(String provider, String model, Whisper whisper) {
    }

    public record Whisper(String provider) {
    }

    public record Cors(List<String> allowedOrigins) {
    }
}
