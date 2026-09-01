package br.com.vidaconecta.ehr.application;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "vida-conecta.ehr")
public record EhrProperties(String encryptionKey) {
}
