package br.com.vidaconecta.shared.infrastructure;

import ch.qos.logback.classic.pattern.ClassicConverter;
import ch.qos.logback.classic.spi.ILoggingEvent;

/**
 * Conversor Logback customizado para mascarar dados sensíveis em tempo de escrita.
 * 
 * <p>Intercepta cada mensagem de log antes de ser escrita no console ou arquivo, 
 * aplicando máscaras automáticas para remover informações pessoais conforme 
 * exigido pela LGPD.</p>
 * 
 * <h3>Configuração no logback-spring.xml:</h3>
 * <pre>{@code
 * <configuration>
 *     <conversionRule conversionWord="maskedMsg" 
 *                     converterClass="br.com.vidaconecta.shared.infrastructure.LogMaskingConverter" />
 *     
 *     <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
 *         <encoder>
 *             <pattern>%d{HH:mm:ss} %level %logger{36} - %maskedMsg%n</pattern>
 *         </encoder>
 *     </appender>
 * </configuration>
 * }</pre>
 * 
 * <h3>Padrões mascarados automaticamente:</h3>
 * <ul>
 *   <li>CPF: ###.###.###-## → ***.***.***.##</li>
 *   <li>Email: user@domain.com → ***@domain.com</li>
 *   <li>Telefone: (11) 9999-9999 → (**) ****-99</li>
 *   <li>UUID: 550e8400-... → 550e8400-****</li>
 * </ul>
 * 
 * <p><strong>Performance:</strong> O conversor utiliza padrões pré-compilados 
 * no {@link MaskUtil} para minimizar overhead em logs de alta frequência.</p>
 * 
 * <p><strong>Conformidade:</strong> Implementa LGPD Art. 5 (Minimização) 
 * e LGPD Art. 9 (Dados Sensíveis).</p>
 * 
 * @author Vida Conecta Team
 * @since 1.0
 * 
 * @see MaskUtil
 * @see ch.qos.logback.classic.pattern.ClassicConverter
 */
public class LogMaskingConverter extends ClassicConverter {

    /**
     * Converte uma mensagem de log aplicando máscaras de dados sensíveis.
     * 
     * <p>Método chamado automaticamente pelo Logback para cada evento de log 
     * que utilize o padrão {@code %maskedMsg}. A mensagem é processada 
     * através do {@link MaskUtil#maskAll(String)} antes de ser retornada.</p>
     * 
     * <h3>Exemplo:</h3>
     * <pre>
     * // Log original
     * log.info("Paciente {} com CPF {} acessou EHR", 
     *          "550e8400-e29b-41d4-a716-446655440000", 
     *          "123.456.789-00");
     *          
     * // Log escrito no arquivo/console
     * "Paciente 550e8400-**** com CPF ***.***.***.00 acessou EHR"
     * </pre>
     * 
     * @param event evento de log fornecido pelo Logback
     * @return mensagem formatizada com dados sensíveis mascarados, 
     *         ou string vazia se evento for null ou sem mensagem
     * 
     * @see ILoggingEvent#getFormattedMessage()
     * @see MaskUtil#maskAll(String)
     */
    @Override
    public String convert(ILoggingEvent event) {
        String message = event.getFormattedMessage();
        if (message == null) {
            return "";
        }
        
        // Mascara todos os tipos conhecidos de dados sensíveis
        return MaskUtil.maskAll(message);
    }
}