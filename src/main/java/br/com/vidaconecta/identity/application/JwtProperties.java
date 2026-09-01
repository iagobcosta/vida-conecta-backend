package br.com.vidaconecta.identity.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vida-conecta.jwt")
public record JwtProperties(String secret, long expirationMinutes) {
}
