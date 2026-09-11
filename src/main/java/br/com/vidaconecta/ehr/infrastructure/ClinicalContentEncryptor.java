package br.com.vidaconecta.ehr.infrastructure;

import br.com.vidaconecta.ehr.application.EhrProperties;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

@Component
public class ClinicalContentEncryptor {

	private static final String TRANSFORMATION = "AES/GCM/NoPadding";
	private static final int GCM_TAG_BITS = 128;
	private static final int IV_LENGTH = 12;

	private final SecretKey secretKey;
	private final SecureRandom secureRandom = new SecureRandom();

	public ClinicalContentEncryptor(EhrProperties properties) {
		String encoded = properties.encryptionKey() == null ? "" : properties.encryptionKey().trim();
		if (encoded.isEmpty() || encoded.startsWith("<") || encoded.contains(" ")) {
			throw new IllegalStateException(
					"EHR_ENCRYPTION_KEY inválida. Gere com `openssl rand -base64 32` e cole só o Base64, sem aspas e sem < >");
		}
		byte[] key;
		try {
			key = Base64.getDecoder().decode(encoded);
		} catch (IllegalArgumentException exception) {
			throw new IllegalStateException(
					"EHR_ENCRYPTION_KEY não é Base64 válido. Gere com `openssl rand -base64 32`",
					exception);
		}
		if (key.length != 32) {
			throw new IllegalStateException("EHR_ENCRYPTION_KEY deve ser Base64 de exatamente 32 bytes (AES-256)");
		}
		this.secretKey = new SecretKeySpec(key, "AES");
	}

	public EncryptedPayload encrypt(String plaintext) {
		try {
			byte[] iv = new byte[IV_LENGTH];
			secureRandom.nextBytes(iv);
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
			return new EncryptedPayload(ciphertext, iv);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Falha ao cifrar conteúdo clínico", exception);
		}
	}

	public String decrypt(byte[] ciphertext, byte[] iv) {
		try {
			Cipher cipher = Cipher.getInstance(TRANSFORMATION);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
			return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
		} catch (GeneralSecurityException exception) {
			throw new IllegalStateException("Falha ao decifrar conteúdo clínico", exception);
		}
	}

	public record EncryptedPayload(byte[] ciphertext, byte[] iv) {
	}
}
