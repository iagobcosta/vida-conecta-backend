package br.com.vidaconecta.shared.api;

public class NotFoundException extends BusinessException {

	public NotFoundException(String message) {
		super(message);
	}
}
