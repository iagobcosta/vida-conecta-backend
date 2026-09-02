package br.com.vidaconecta;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

import br.com.vidaconecta.shared.infrastructure.MaskUtil;

/**
 * Testes unitários para {@link MaskUtil}.
 * 
 * <p>Valida que o mascaramento de dados sensíveis (CPF, email, telefone, UUID) 
 * funciona corretamente e mantém a segurança exigida pela LGPD.</p>
 * 
 * @author Vida Conecta Team
 * @since 1.0
 * 
 * @see MaskUtil
 */
class MaskUtilTest {

	/**
	 * Testa mascaramento de CPF no formato padrão brasileiro.
	 * 
	 * <p>Valida que CPF "123.456.789-00" é mascarado para "***.***.***.00", 
	 * preservando apenas os 2 últimos dígitos.</p>
	 */
	@Test
	void shouldMaskCpf() {
		String result = MaskUtil.maskCpf("123.456.789-00");
		assertEquals("***.***.***.00", result);
	}

	/**
	 * Testa mascaramento de email.
	 * 
	 * <p>Valida que email "paciente@example.com" é mascarado para 
	 * "***@example.com", removendo completamente a parte local e 
	 * preservando domínio e TLD.</p>
	 */
	@Test
	void shouldMaskEmail() {
		String result = MaskUtil.maskEmail("paciente@example.com");
		assertEquals("***@example.com", result);
	}

	/**
	 * Testa mascaramento de telefone com formatação.
	 * 
	 * <p>Valida que telefone "(11) 9 1234-5678" é mascarado para "(**) * ****-78", 
	 * preservando apenas os 2 últimos dígitos e estrutura.</p>
	 */
	@Test
	void shouldMaskPhone() {
		String result = MaskUtil.maskPhone("(11) 9 1234-5678");
		assertEquals("(**) * ****-78", result);
	}

	/**
	 * Testa mascaramento de UUID.
	 * 
	 * <p>Valida que UUID "550e8400-e29b-41d4-a716-446655440000" é mascarado 
	 * para "550e8400-****", preservando apenas os primeiros 8 caracteres.</p>
	 */
	@Test
	void shouldMaskUUID() {
		String result = MaskUtil.maskUUID("550e8400-e29b-41d4-a716-446655440000");
		assertEquals("550e8400-****", result);
	}

	/**
	 * Testa mascaramento de UUID com letras maiúsculas.
	 * 
	 * <p>Valida que UUID com maiúsculas é tratado corretamente (case-insensitive) 
	 * e mantém os primeiros 8 caracteres em maiúsculas.</p>
	 */
	@Test
	void shouldMaskUUIDCaseInsensitive() {
		String result = MaskUtil.maskUUID("550E8400-E29B-41D4-A716-446655440000");
		assertEquals("550E8400-****", result);
	}

	/**
	 * Testa mascaramento de múltiplos tipos de dados em um texto.
	 * 
	 * <p>Valida que {@link MaskUtil#maskAll(String)} processa corretamente 
	 * um texto contendo email, CPF e UUID simultaneamente, mascarando todos.</p>
	 */
	@Test
	void shouldMaskAllTypes() {
		String text = "Usuário pablito@example.com (CPF 123.456.789-00) acessou dados do paciente 550e8400-e29b-41d4-a716-446655440000";
		String result = MaskUtil.maskAll(text);

		// Valida que dados originais foram removidos
		assertFalse(result.contains("pablito@example.com"));
		assertFalse(result.contains("123.456.789-00"));
		assertFalse(result.contains("550e8400-e29b-41d4-a716-446655440000"));

		// Valida que dados mascarados estão presentes
		assertTrue(result.contains("***@example.com"));
		assertTrue(result.contains("***.***.***.00"));
		assertTrue(result.contains("550e8400-****"));
	}

	/**
	 * Testa tratamento de valores nulos.
	 * 
	 * <p>Valida que todos os métodos de mascaramento retornam valores padrão 
	 * seguros quando recebem null.</p>
	 */
	@Test
	void shouldHandleNullValues() {
		assertEquals("***", MaskUtil.maskCpf(null));
		assertEquals("***", MaskUtil.maskPhone(null));
		assertEquals("***@***", MaskUtil.maskEmail(null));
		assertEquals("****", MaskUtil.maskUUID(null));
	}

	/**
	 * Testa tratamento de strings vazias.
	 * 
	 * <p>Valida que strings vazias são tratadas como inválidas e retornam 
	 * valores padrão seguros.</p>
	 */
	@Test
	void shouldHandleEmptyStrings() {
		assertEquals("***", MaskUtil.maskCpf(""));
		assertEquals("***@***", MaskUtil.maskEmail(""));
		assertEquals("***", MaskUtil.maskPhone(""));
		assertEquals("****", MaskUtil.maskUUID(""));
	}

	/**
	 * Testa mascaramento preservando segurança mínima.
	 * 
	 * <p>Valida que o mascaramento mantém apenas os últimos 2 dígitos, 
	 * não expondo mais informação que o necessário.</p>
	 */
	@Test
	void shouldPreserveMinimalInfo() {
		String cpf = MaskUtil.maskCpf("111.222.333-44");
		assertTrue(cpf.endsWith("44"), "CPF deve terminar com últimos 2 dígitos");
		assertEquals(14, cpf.length(), "CPF mascarado deve manter comprimento original");
	}
}
