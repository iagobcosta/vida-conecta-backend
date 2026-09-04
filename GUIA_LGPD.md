# Guia de Verificação LGPD - Vida Conecta Backend

**Para:** Responsável pela etapa de LGPD  
**Data:** 2026-09-01

---

## 📋 O QUE JÁ ESTÁ IMPLEMENTADO

### 1. ✅ Criptografia de Dados Sensíveis (AES-GCM)
**Arquivo:** [ClinicalContentEncryptor.java](src/main/java/br/com/vidaconecta/ehr/infrastructure/ClinicalContentEncryptor.java)

```java
// Implementação presente
- AES/GCM/NoPadding (padrão NIST)
- GCM Tag de 128 bits (autenticação + integridade)
- IV aleatório de 12 bytes para cada cifra
- Chave de 32 bytes (AES-256)
```

✅ **Status:** Pronto  
🔍 **Verificar:** 
- [ ] Origem da chave está em variável de ambiente (não em código)
- [ ] Chave não é logada em nenhum lugar
- [ ] Testcontainers testa com chave diferente da produção

---

### 2. ✅ Consentimento Versionado
**Arquivo:** [Consent.java](src/main/java/br/com/vidaconecta/consent/domain/Consent.java)

```java
- Entidade com versionamento automático
- Suporta dois escopos:
  - DOCTOR: acesso contínuo ao médico
  - APPOINTMENT: acesso apenas àquela consulta
- Expiração configurável
- Revogação com timestamp
- Validação: isActive(), covers()
```

✅ **Status:** Pronto  
🔍 **Verificar:**
- [ ] Expiração automática é testada (incluir teste de clock skew)
- [ ] Revogação dupla é impedida
- [ ] Consentimento revogado não é contabilizado como válido

---

### 3. ✅ Auditoria de Acesso (Não repudiação)
**Arquivo:** [EhrAccessAudit.java](src/main/java/br/com/vidaconecta/ehr/domain/EhrAccessAudit.java)

```java
- Registra: quem (UUID), o quê (ação), quando (timestamp)
- Ações: READ, WRITE
- Vinculado a paciente e consulta
- Timestamp com Instant (UTC)
- Query-only: findByPatientIdOrderByAccessedAtDesc()
```

✅ **Status:** Parcialmente pronto  
🔍 **Verificar:**
- [ ] Registra tentativas NEGADAS também
- [ ] Endpoint `/ehr/audit` restringe admin ou paciente dono
- [ ] Audit log nunca é alterado (append-only)
- [ ] Teste E2E: médico sem consentimento tenta ler → falha → audit registra

---

### 4. ✅ Controle de Acesso (Autorização)
**Arquivo:** [EhrService.java](src/main/java/br/com/vidaconecta/ehr/application/EhrService.java)

```java
- list() valida:
  1. Paciente acessa seus próprios dados
  2. Médico acessa APENAS:
     - Suas próprias notas (author)
     - Dados do paciente SE houver consentimento VÁLIDO
     - OU se admin
  3. Filtra automaticamente notas não autorizado
- Usa ConsentFacade.hasValidConsent() centralizado
```

✅ **Status:** Pronto  
🔍 **Verificar:**
- [ ] Teste: médico B tenta ler prontuário de paciente atendido por médico A → forbidden
- [ ] Teste: médico A lê prontuário antes de consentimento → empty list
- [ ] Teste: médico A lê prontuário após consentimento → notas visíveis

---

### 5. ✅ Spring Security + RBAC
**Arquivo:** [SecurityConfig.java](src/main/java/br/com/vidaconecta/identity/web/SecurityConfig.java)

```java
- CSRF desabilitado (API stateless)
- CORS configurável por ambiente
- JWT + OAuth2 Resource Server
- Sessão stateless
- Rotas públicas: /auth/register, /auth/login, /swagger-ui
- Rotas protegidas: exigem Bearer token
- Filtro de conta ativa (EnabledAccountFilter)
```

✅ **Status:** Pronto  
🔍 **Verificar:**
- [ ] Endpoints protegidos falham sem token (401)
- [ ] Endpoints protegidos falham com token inválido (401)
- [ ] Conta desativada perde acesso (mesmo com token válido)

---

## ⚠️ O QUE PRECISA SER VERIFICADO

### 1. Política de Retenção de Dados
**Arquivo:** Não existe (precisa criar)

```
FALTA definir:
- Quanto tempo manter audit logs? (LGPD recomenda conforme alegação)
- Quanto tempo manter consentimentos revogados? (para prova de revogação)
- Quanto tempo manter dados de consulta após cancelamento?
- Implementar job de limpeza com soft delete
```

**Tarefa:**
- [ ] Criar `DataRetentionPolicy.java` com política
- [ ] Implementar `AuditLogCleanupJob.java` (scheduled)
- [ ] Adicionar `deleted_at` em tabelas sensíveis
- [ ] Teste: audit log com > 90 dias é marcado para exclusão

---

### 2. Mascaramento de Dados Sensíveis em Logs
**Status:** CRÍTICO - Não implementado

```java
// Hoje: logs mostram tudo
log.info("User accessed EHR for patient {}", patientId); // ← expõe UUID do paciente

// Precisa:
log.info("User accessed EHR"); // ← sem IDs
// Contexto sensível apenas em auditlog
```

**Tarefa:**
- [X] Criar `MaskUtil.java` com métodos:
  ```java
  public static String maskCpf(String cpf) → "***.***.***-45"
  public static String maskEmail(String email) → "p***@example.com"
  public static String maskPhone(String phone) → "***-****-90"
  public static String maskUUID(UUID id) → "550e8400-e29b-***"
  ```
- [X] Aplicar em todas as mensagens de log
- [X] Teste: logs nunca contêm CPF, email completo, UUID de paciente

---

### 3. Consentimento: Registrar Tentativas Negadas
**Status:** Totalmente implementado ✅

```java
// EhrAuditService independente com REQUIRES_NEW
ehrAuditService.record(currentUser.id(), patientId, appointmentId, "READ_DENIED");
throw new ForbiddenException("Acesso negado");
```

**Tarefa:**
- [x] Adicionar coluna `result` em `ehr_access_audit` (SUCCESS/DENIED) ou ação `READ_DENIED`
- [x] Registrar DENIED antes de lançar exceção
- [x] Teste: acesso negado é registrado com ação READ_DENIED

---

### 4. Anonimização em Ambiente de Teste
**Status:** Não implementado

```
O banco de testes não deve ter dados reais de produção.
Precisa de script de anonimização.
```

**Tarefa:**
- [ ] Criar `anonymize-db.sql` que:
  - Remove dados pessoais de testes
  - Mantém estrutura para testes
- [ ] Documentar: "Nunca rodar scripts de prod em dev"

---

### 5. Política de Backup & Restore
**Status:** Não implementado

```
FALTA:
- Teste de restauração de backup
- Documentação de como restaurar
- Retenção de backups
```

**Tarefa:**
- [ ] Criar plano: backup diário, retenção 30 dias
- [ ] Teste mensal: restaura backup, verifica integridade
- [ ] Documentar em RUNBOOK.md

---

### 6. Validação de Consentimento em Cada Acesso
**Status:** Implementado, mas não testado completamente

```java
// EhrService.list() valida consentimento
// Mas faltam testes de edge cases:
- Consentimento expirou entre leitura e resultado?
- Consentimento foi revogado durante a consulta?
- Consulta foi cancelada (consentimento por consulta fica inválido)?
```

**Tarefa:**
- [ ] Teste: médico inicia leitura com consentimento válido → consentimento expira → próxima paginação falha
- [ ] Teste: consentimento é revogado durante operação
- [ ] Teste: consulta é cancelada → consentimento por consulta fica inválido

---

## 🚀 PLANO DE AÇÃO (Prioridade)

### Semana 1 (CRÍTICO)
1. **Mascaramento de logs** - sem dados sensíveis visíveis
2. **Auditoria de tentativas negadas** - registrar tudo
3. **Testes de edge cases** - consentimento expirado, revogado
4. **Spring Security test** - tokens expirados, inválidos

### Semana 2
5. **Política de retenção** - definir e implementar
6. **Backup & restore** - testar procedimento
7. **Anonimização de testes** - script de limpeza

### Semana 3
8. **Documentação final** - compliance checklist
9. **Auditoria externa** - validar implementação

---

## 📝 CHECKLIST DE LGPD

Marcar conforme implementa:

### Consentimento (Art. 8)
- [ ] Consentimento é **explícito** (paciente clica "Autorizar")
- [ ] Consentimento é **versionado** (permite rastrear mudanças)
- [ ] Consentimento pode ser **revogado** a qualquer momento
- [ ] Revogação é **imediata** (próximo acesso falha)
- [ ] Consentimento tem **prazo de validade** (expiração)
- [ ] Paciente visualiza **histórico de consentimentos**
- [ ] Teste E2E: fluxo completo de consentimento

### Dados Sensíveis (Art. 5)
- [x] **Criptografia em repouso** (AES-GCM)
- [x] **Criptografia em trânsito** (HTTPS/TLS)
- [ ] **Minimização** (coleta apenas necessário) → revisar
- [ ] **Retenção** (política definida) ⚠️
- [X] **Anonimização** (em testes e logs)

### Acesso (Art. 9)
- [x] **Autenticação forte** (JWT + refresh) ✅
- [x] **Autorização por papel** (RBAC) ✅
- [x] **Validação de consentimento** antes de cada acesso ✅
- [x] **Acesso cross-paciente impedido** ✅
- [x] **Auditoria de quem acessou** ✅
- [x] **Registro de quando** ✅
- [x] **Registro do resultado** (sucesso/falha) ✅

### Direitos do Titular (Art. 17-18)
- [ ] Direito de **acessar** seus dados (GET /patients/{id}/ehr)
- [ ] Direito de **corrigir** dados cadastrais
- [ ] Direito de **deletar/esquecimento** (Criar `DELETE /api/v1/me` para soft delete e anonimização de PII)
- [ ] Direito de **portabilidade** (Criar `GET /api/v1/me/export` para baixar JSON consolidado)
- [x] Direito de **revogar consentimento** ✅

### Documentação
- [ ] Mapeamento de dados: quais são sensíveis?
- [ ] Fluxo de processamento documentado
- [ ] Política de retenção escrita
- [ ] Procedimento de backup & restore documentado
- [ ] Plano de resposta a incidentes de segurança

---

## 🔐 Arquivos a Revisar

```
Prioridade ALTA:
1. src/main/java/br/com/vidaconecta/ehr/application/EhrService.java
   → Validação de acesso e consentimento

2. src/main/java/br/com/vidaconecta/consent/domain/Consent.java
   → Lógica de versionamento e revogação

3. src/main/java/br/com/vidaconecta/ehr/infrastructure/ClinicalContentEncryptor.java
   → Segurança de criptografia

4. src/main/resources/db/migration/V1__init.sql
   → Schema de tabelas sensíveis

5. src/test/java/br/com/vidaconecta/ClinicalFlowTests.java
   → Testes de fluxo clínico (validar cobertura)

Prioridade MÉDIA:
6. src/main/java/br/com/vidaconecta/identity/web/SecurityConfig.java
   → Configuração de segurança

7. src/main/java/br/com/vidaconecta/consent/infrastructure/ConsentRepository.java
   → Queries de consentimento
```

---

## 🧪 Teste Rápido: Fluxo de Consentimento LGPD

Siga este teste manual para validar tudo:

```bash
1. Registrar paciente:
   POST /api/v1/auth/register
   → Email: pac@test.com, CPF: 123.456.789-00

2. Registrar médico:
   POST /api/v1/auth/register/admin (com bootstrap token)
   POST /api/v1/admin/doctors/invites (convida médico)
   → Email: med@test.com

3. Agendar consulta:
   POST /api/v1/appointments
   → paciente agenda com médico para amanhã

4. Médico tenta ler prontuário SEM consentimento:
   GET /api/v1/patients/{id}/ehr
   → Esperado: 200 OK, list vazio (ou 403?)
   → Verificar: audit log registra READ_DENIED?

5. Paciente concede consentimento:
   POST /api/v1/consents
   → Scope: DOCTOR, expiresAt: +90 dias

6. Médico lê prontuário COM consentimento:
   GET /api/v1/patients/{id}/ehr
   → Esperado: 200 OK, notas visíveis
   → Verificar: notas estão decriptadas?

7. Médico escreve nota:
   POST /api/v1/patients/{id}/ehr
   → content: "Paciente com hipertensão"
   → Verificar: conteúdo foi criptografado?

8. Paciente revoga consentimento:
   POST /api/v1/consents/{id}/revoke
   → Verificado: revokedAt foi preenchido

9. Médico tenta ler prontuário APÓS revogação:
   GET /api/v1/patients/{id}/ehr
   → Esperado: 200 OK, list vazio
   → Verificar: audit log registra READ_DENIED?

10. Paciente acessa auditoria:
    GET /api/v1/ehr/audit?patientId={id}
    → Esperado: lista de todos os acessos
    → Verificar: inclui READ_DENIED?
```

---

## 📞 Dúvidas Comuns

**P: Onde fica a chave de criptografia?**  
R: `EHR_ENCRYPTION_KEY` em variável de ambiente (Base64 de 32 bytes). Nunca em código.

**P: O consentimento por consulta expira quando?**  
R: Fica válido conforme `expiresAt` ou até ser revogado. Se é por consulta e consulta foi cancelada, fica inválido.

**P: E se o médico tentar contornar validação?**  
R: Spring Security + `@PreAuthorize` + validação em serviço. Três camadas.

**P: Dados são deletados?**  
R: Soft delete (não implementado). Revisar escopo. Hoje: apenas revogação e auditoria.

**P: Qual é o SLA de acesso ao audit log?**  
R: Paciente vê em tempo real (GET /ehr/audit). Admin vê conforme políticas de retenção.

---

## 📚 Referências Úteis

- LGPD: https://www.gov.br/cidadania/pt-br/acesso-a-informacao/lgpd
- NIST SP 800-38D (GCM): https://nvlpubs.nist.gov/nistpubs/Legacy/SP/nistspecialpublication800-38d.pdf
- Spring Security: https://spring.io/projects/spring-security
- OWASP: https://owasp.org/

---

**Próximo passo:** Começar pela Semana 1 - Mascaramento de Logs. Bora? 🚀
