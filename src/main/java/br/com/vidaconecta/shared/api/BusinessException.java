package br.com.vidaconecta.shared.api;

public class BusinessException extends RuntimeException {

	public BusinessException(String message) {
		super(message);
	}
}
