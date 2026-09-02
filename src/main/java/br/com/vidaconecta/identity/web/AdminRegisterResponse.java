package br.com.vidaconecta.identity.web;

import java.util.UUID;

public record AdminRegisterResponse(String token, String tokenType, UUID nextBootstrapToken) {

	public AdminRegisterResponse(String token, UUID nextBootstrapToken) {
		this(token, "Bearer", nextBootstrapToken);
	}
}
