package br.com.vidaconecta.shared.api;

public class ForbiddenException extends BusinessException {

	public ForbiddenException(String message) {
		super(message);
	}
}
