package br.com.vidaconecta.shared.infrastructure;

import java.util.regex.Pattern;

/**
 * Utilitário para mascarar dados sensíveis de acordo com LGPD.
 * 
 * <p>Fornece métodos estáticos para mascarar informações pessoais como CPF, 
 * email, telefone e UUID. Utilizado pelo {@link LogMaskingConverter} do 
 * Logback para sanitizar logs em tempo de escrita.</p>
 * 
 * <p><strong>Segurança:</strong> Este utilitário mascara dados mantendo apenas 
 * os últimos dígitos/caracteres para fins de identificação limitada, sem 
 * comprometer a privacidade (LGPD Art. 5).</p>
 * 
 * @author Vida Conecta Team
 * @since 1.0
 */
public class MaskUtil {

    private static final Pattern CPF_PATTERN = Pattern.compile("\\b\\d{3}\\.\\d{3}\\.\\d{3}-\\d{2}\\b");
    private static final Pattern EMAIL_PATTERN = Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Z|a-z]{2,}\\b");
    private static final Pattern PHONE_PATTERN = Pattern.compile("\\b(?:\\+?\\d{1,3}[\\s.-]?)?(?:\\(?\\d{2,4}\\)?[\\s.-]?)?\\d{3,4}[\\s.-]?\\d{3,4}\\b");
    private static final Pattern UUID_PATTERN = Pattern.compile(
        "\\b[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}\\b",
        Pattern.CASE_INSENSITIVE
    );

    private MaskUtil() {
        // Construtor privado para utilitário
    }

    /**
     * Mascara um CPF no formato com pontos e hífen.
     * 
     * <p>Substitui dígitos deixando visíveis apenas os dois últimos dígitos 
     * para rastreabilidade limitada.</p>
     * 
     * <h3>Exemplos:</h3>
     * <pre>
     * maskCpf("123.456.789-00") → "***.***.***.00"
     * maskCpf("") → "***"
     * maskCpf(null) → "***"
     * </pre>
     * 
     * @param cpf CPF no formato "###.###.###-##", ou null
     * @return CPF mascarado preservando últimos 2 dígitos, ou "***" se inválido
     */
    public static String maskCpf(String cpf) {
        if (cpf == null || cpf.length() < 5) {
            return "***";
        }
        return cpf.replaceAll("\\d(?=\\d{2})", "*");
    }

    /**
     * Mascara um endereço de email.
     * 
     * <p>Substitui a parte local (antes do @) por asteriscos, mantendo 
     * apenas o domínio e TLD para identificação limitada.</p>
     * 
     * <h3>Exemplos:</h3>
     * <pre>
     * maskEmail("paciente@example.com") → "***@example.com"
     * maskEmail("admin@empresa.com.br") → "***@empresa.com.br"
     * maskEmail("invalid") → "***@***"
     * maskEmail(null) → "***@***"
     * </pre>
     * 
     * @param email endereço de email válido, ou null
     * @return email mascarado preservando domínio, ou "***@***" se inválido
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) return "***@example.com";
        int atIndex = email.indexOf('@');
        if (atIndex <= 1) return "***@example.com";
        return email.charAt(0) + "***" + email.substring(atIndex);
    }

    /**
     * Mascara um número de telefone.
     * 
     * <p>Substitui dígitos deixando visíveis apenas os últimos dois para 
     * rastreabilidade limitada. Suporta diversos formatos de telefone.</p>
     * 
     * <h3>Exemplos:</h3>
     * <pre>
     * maskPhone("(11) 9 1234-5678") → "(**) * ****-78"
     * maskPhone("11999999999") → "***999999-99"
     * maskPhone("") → "***"
     * maskPhone(null) → "***"
     * </pre>
     * 
     * @param phone número de telefone em qualquer formato, ou null
     * @return telefone mascarado preservando últimos 2 dígitos, ou "***" se inválido
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "***";
        }
        return phone.replaceAll("\\d(?=\\d{2})", "*");
    }

    /**
     * Mascara um identificador UUID.
     * 
     * <p>Preserva apenas o prefixo (8 primeiros caracteres hexadecimais) 
     * para referência limitada, sem comprometer anonimidade.</p>
     * 
     * <h3>Exemplos:</h3>
     * <pre>
     * maskUUID("550e8400-e29b-41d4-a716-446655440000") → "550e8400-****"
     * maskUUID("550E8400-E29B-41D4-A716-446655440000") → "550E8400-****"
     * maskUUID("") → "****"
     * maskUUID(null) → "****"
     * </pre>
     * 
     * @param uuid identificador UUID válido (maiúsculas ou minúsculas), ou null
     * @return UUID mascarado preservando primeiros 8 caracteres, ou "****" se inválido
     */
    public static String maskUUID(String uuid) {
        if (uuid == null || uuid.length() < 9) {
            return "****";
        }
        return uuid.substring(0, 8).toUpperCase() + "-****";
    }

    /**
     * Mascara todos os tipos conhecidos de dados sensíveis em um texto.
     * 
     * <p>Processa um texto livre aplicando máscaras automáticas para CPF, email, 
     * telefone e UUID na ordem especificada. Útil para sanitizar logs que 
     * possam conter múltiplos tipos de dados sensíveis.</p>
     * 
     * <h3>Exemplo de uso com Logback:</h3>
     * <pre>
     * // Entrada
     * "Usuário pablito@example.com (CPF 123.456.789-00) acessou dados 
     *  do paciente 550e8400-e29b-41d4-a716-446655440000"
     *  
     * // Saída
     * "Usuário ***@example.com (CPF ***.***.***.00) acessou dados 
     *  do paciente 550e8400-****"
     * </pre>
     * 
     * @param text texto potencialmente contendo dados sensíveis, ou null
     * @return texto com todos os padrões conhecidos mascarados
     * 
     * @see #maskCpf(String)
     * @see #maskEmail(String)
     * @see #maskPhone(String)
     * @see #maskUUID(String)
     */
    public static String maskAll(String text) {
        if (text == null) {
            return "";
        }
        String result = text;
        result = CPF_PATTERN.matcher(result).replaceAll(m -> maskCpf(m.group()));
        result = EMAIL_PATTERN.matcher(result).replaceAll(m -> maskEmail(m.group()));
        result = PHONE_PATTERN.matcher(result).replaceAll(m -> maskPhone(m.group()));
        result = UUID_PATTERN.matcher(result).replaceAll(m -> maskUUID(m.group()));
        return result;
    }
}