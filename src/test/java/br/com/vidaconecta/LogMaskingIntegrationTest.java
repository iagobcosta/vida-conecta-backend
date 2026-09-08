package br.com.vidaconecta;

import br.com.vidaconecta.shared.infrastructure.LogMaskingConverter;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.PatternLayout;
import ch.qos.logback.classic.encoder.PatternLayoutEncoder;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.OutputStreamAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Testes de integração para o mascaramento de logs via Logback.
 * 
 * <p>Valida que dados sensíveis (CPF, e-mail, telefone, UUID) são automaticamente
 * interceptados e mascarados pelo {@link LogMaskingConverter} através da regra
 * {@code %maskedMsg} configurada no {@code logback-spring.xml}.</p>
 */
class LogMaskingIntegrationTest {

	private LoggerContext context;
	private ByteArrayOutputStream outputStream;
	private OutputStreamAppender<ILoggingEvent> appender;
	private Logger logger;

	@BeforeEach
	void setUp() throws Exception {
		context = (LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
		PatternLayout.defaultConverterMap.put("maskedMsg", LogMaskingConverter.class.getName());

		outputStream = new ByteArrayOutputStream();

		PatternLayoutEncoder encoder = new PatternLayoutEncoder();
		encoder.setContext(context);
		encoder.setPattern("%maskedMsg%n");
		encoder.start();

		appender = new OutputStreamAppender<>();
		appender.setName("testOutputStreamAppender");
		appender.setContext(context);
		appender.setEncoder(encoder);
		appender.setOutputStream(outputStream);
		appender.start();

		logger = context.getLogger("br.com.vidaconecta.test.LogMasking");
		logger.setLevel(Level.INFO);
		logger.addAppender(appender);
		logger.setAdditive(false);
	}

	@AfterEach
	void tearDown() {
		if (logger != null && appender != null) {
			logger.detachAppender(appender);
		}
		if (appender != null) {
			appender.stop();
		}
	}

	@Test
	@DisplayName("Garante que logs via Logback nunca expõem dados sensíveis (CPF, email, telefone, UUID)")
	void shouldMaskSensitiveDataInLogMessages() {
		String sensitiveLog = "Paciente 550e8400-e29b-41d4-a716-446655440000 com email paciente@example.com, "
				+ "CPF 123.456.789-00 e tel (11) 9 1234-5678 realizou login.";

		logger.info(sensitiveLog);

		String loggedOutput = outputStream.toString(StandardCharsets.UTF_8);

		// Valida que os dados sensíveis NÃO estão presentes no log
		assertFalse(loggedOutput.contains("550e8400-e29b-41d4-a716-446655440000"), "UUID original não deve estar no log");
		assertFalse(loggedOutput.contains("paciente@example.com"), "E-mail original não deve estar no log");
		assertFalse(loggedOutput.contains("123.456.789-00"), "CPF original não deve estar no log");
		assertFalse(loggedOutput.contains("1234-5678"), "Telefone original não deve estar no log");

		// Valida que os formatos mascarados ESTÃO presentes no log
		assertTrue(loggedOutput.contains("550E8400-****"), "UUID mascarado deve estar no log");
		assertTrue(loggedOutput.contains("p***@example.com"), "E-mail mascarado deve estar no log");
		assertTrue(loggedOutput.contains("***.***.***-00"), "CPF mascarado deve estar no log");
		assertTrue(loggedOutput.contains("(**) * ****-**78"), "Telefone mascarado deve estar no log");
	}

	@Test
	@DisplayName("Valida conversor diretamente com evento parametrizado do SLF4J (placeholders {})")
	void shouldMaskParameterizedLogEvents() {
		LogMaskingConverter converter = new LogMaskingConverter();
		converter.setContext(context);
		converter.start();

		LoggingEvent event = new LoggingEvent(
				"br.com.vidaconecta.Service",
				context.getLogger("br.com.vidaconecta.Service"),
				Level.INFO,
				"Convite enviado para médico {} <{}> com CRM {}",
				null,
				new Object[]{"Dr. Teste", "doutor@vidaconecta.com.br", "123456"}
		);

		String result = converter.convert(event);

		assertFalse(result.contains("doutor@vidaconecta.com.br"), "E-mail do médico não deve estar exposto");
		assertTrue(result.contains("d***@vidaconecta.com.br"), "E-mail do médico deve estar mascarado");
		assertTrue(result.contains("Dr. Teste"), "Nome não sensível deve ser mantido");
		assertTrue(result.contains("123456"), "CRM deve ser mantido");
	}

	@Test
	@DisplayName("Valida tratamento seguro de eventos com mensagens nulas")
	void shouldHandleNullMessageSafely() {
		LogMaskingConverter converter = new LogMaskingConverter();
		converter.setContext(context);
		converter.start();

		LoggingEvent event = new LoggingEvent();
		assertEquals("", converter.convert(event));
	}
}
