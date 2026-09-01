package br.com.vidaconecta.identity.web;

public record TokenResponse(String token, String tokenType) {

	public TokenResponse(String token) {
		this(token, "Bearer");
	}
}
