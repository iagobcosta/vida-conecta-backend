package br.com.vidaconecta.shared.api;

public final class PersonalDataMask {

	private PersonalDataMask() {
	}

	public static String cpf(String cpf) {
		if (cpf == null || cpf.isBlank()) {
			return null;
		}
		String digits = cpf.replaceAll("\\D", "");
		if (digits.length() < 2) {
			return "***";
		}
		return "***.***.***-" + digits.substring(digits.length() - 2);
	}
}
