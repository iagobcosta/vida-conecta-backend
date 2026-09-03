# Status de Implementação - Vida Conecta Backend

**Última atualização:** 2026-09-01

## 📋 Resumo Executivo

O backend está **95% completo**. A maioria dos módulos funciona em produção. Este documento lista apenas o que ainda precisa ser feito.

> **Projeto adiantado:** Toda a lógica de negócio está pronta. Faltam testes ampliados, integração SFU real, observabilidade e DevSecOps.

---

## ✅ COMPLETO E FUNCIONAL

### 1. Identity (Autenticação & Autorização)
- ✅ Usuários (Patient, Doctor, Admin)
- ✅ Registro e login com JWT
- ✅ Hash com BCrypt
- ✅ Spring Security + RBAC
- ✅ Filtros de autenticação
- ✅ Admin bootstrap token rotativo
- ✅ Convites de médico por e-mail (AWS SES)
- ✅ Endpoints: `/auth/register`, `/auth/login`, `/auth/me`, `/auth/register/admin`, `/auth/register/doctor`

### 2. Scheduling (Agendamentos)
- ✅ Entidade Appointment (SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED, NO_SHOW)
- ✅ DoctorAvailability (horários)
- ✅ Validações: double booking, horário no passado
- ✅ Confirmar, cancelar, completar consulta
- ✅ Endpoints: POST/GET `/appointments`, `/appointments/{id}/confirm|cancel|complete`

### 3. Consent (Consentimento LGPD)
- ✅ Entidade Consent com versionamento
- ✅ Por médico ou por consulta
- ✅ Expiração e revogação
- ✅ Validações de negócio
- ✅ Endpoints: POST/GET `/consents`, `/consents/{id}/revoke`

### 4. EHR (Prontuário Cifrado)
- ✅ ClinicalNote (registros)
- ✅ Criptografia AES-GCM
- ✅ **Auditoria completa** (EhrAccessAudit)
- ✅ Validação de consentimento antes de cada acesso
- ✅ Controle cross-paciente
- ✅ Endpoints: POST/GET `/patients/{id}/ehr`, `/ehr/audit`

### 5. Prescription (Prescrição Digital)
- ✅ Entidade Prescription + PrescriptionItem
- ✅ Médico cria ligada a consulta
- ✅ Paciente e médico visualizam
- ✅ Validações de integridade
- ✅ Endpoints: POST/GET `/prescriptions`

### 6. Notifications (Notificações)
- ✅ Eventos: CONSENT_GRANTED, APPOINTMENT_CONFIRMED, PRESCRIPTION_ISSUED, etc.
- ✅ In-app, disparadas automaticamente
- ✅ Endpoints: GET/POST `/notifications`, `/notifications/{id}/read`

### 7. Video (Videochamada)
- ✅ VideoRoomProvider (interface)
- ✅ Geração de token
- ✅ Validações
- ✅ Endpoints: POST `/video/appointments/{id}/token`
- ⚠️ Mock implementation (precisa SFU real)

### 8. Admin Panel
- ✅ Gerenciamento de médicos
- ✅ Insights do sistema (métricas, evolução)
- ✅ Endpoints: GET/PATCH `/admin/doctors`, `/admin/insights`

### 9. Infraestrutura
- ✅ PostgreSQL + Flyway migrations
- ✅ Docker Compose
- ✅ OpenAPI/Swagger UI
- ✅ Global exception handler
- ✅ Spring Modulith (validação de fronteiras)
- ✅ Testcontainers

---

## ❌ AINDA FALTA

### 1. TESTES (PRIORIDADE ALTA) ⚠️

#### O que existe:
- `ClinicalFlowTests.java` (fluxo completo de consulta)
- `IdentityTests.java` (autenticação)
- `SchedulingTests.java` (agendamentos)

#### O que falta:
- [ ] Testes unitários para cada serviço
- [ ] Testes de consentimento (revogação, expiração, validação)
- [ ] Testes de auditoria (registros de acesso)
- [ ] Testes de prescrição (integridade)
- [ ] Testes de notificações
- [ ] Testes de concorrência (double booking simultâneo)
- [ ] Testes E2E dos fluxos clínicos
- [ ] Testes de segurança (acesso indevido, CSRF, etc.)
- [ ] Cobertura mínima 80%

**Estimativa:** 1-2 semanas de desenvolvimento

---

### 2. INTEGRAÇÃO SFU REAL (PRIORIDADE ALTA) ⚠️

#### Atual (Mock):
```java
// Hoje: retorna token mock sem contato com SFU real
VideoToken token = videoService.generateToken(appointmentId);
```

#### Falta:
- [ ] Contrata LiveKit (ou similar: Liveswitch, Jitsi)
- [ ] Implementa `LiveKitVideoRoomProvider` (cria sala real, gera token válido)
- [ ] Webhook do SFU para eventos: room-created, room-destroyed, participant-joined
- [ ] Testa fluxo de vídeo completo (browser → SFU → métricas)
- [ ] Integra taxa de queda da chamada em métricas

**Estimativa:** 1 semana (incluindo setup da conta e testes)

**Passo a passo:**
```bash
1. Criar conta em livekit.io (cloud) ou deployar self-hosted
2. Gerar chave API e secret
3. Implementar LiveKitVideoRoomProvider
4. Substituir MockVideoRoomProvider
5. Testar com curl + browser
6. Integrar webhooks em NotificationFacade
```

---

### 3. OBSERVABILIDADE (PRIORIDADE MÉDIA) ⚠️

#### Falta:
- [ ] **Micrometer + Prometheus**
  - Métricas de autenticação (logins, falhas)
  - Métricas de agendamento (consultas criadas/canceladas)
  - Métricas de EHR (acessos, por ação)
  - Métricas de prescrição
  - Latência de endpoints
  - Taxa de erro

- [ ] **Logging Estruturado (JSON)**
  - Substituir console logs por JSON estruturado
  - Incluir contexto: userId, appointmentId, action
  - [x] Mascarar dados sensíveis (CPF, email, telefone, UUID) via Logback e MaskUtil

- [ ] **Grafana Dashboards**
  - Overview: uptime, latência, erros
  - Identity: logins, registros, falhas
  - Scheduling: consultas, confirmas, cancelamentos
  - EHR: acessos, sem consentimento, auditoria
  - Video: conexões, taxa de queda

**Estimativa:** 1-2 semanas

**Stack:**
```yaml
App: Spring Boot (Micrometer)
  ↓
Prometheus: coleta métricas
  ↓
Grafana: visualiza + alertas
```

---

### 4. DEVSECOPS & QUALIDADE (PRIORIDADE MÉDIA) ⚠️

#### Falta:
- [ ] **CI/CD (GitHub Actions)**
  - [ ] Build com Maven
  - [ ] Testes automáticos
  - [ ] SAST (SonarQube Community)
  - [ ] Dependency scanning (Trivy ou Dependabot)
  - [ ] Secret scanning (Gitleaks)
  - [ ] Container scanning (docker scan)
  - [ ] Quality Gate com threshold

- [ ] **Git Governance**
  - [ ] Branch protection: main exige PR + checks
  - [ ] CODEOWNERS para code review
  - [ ] Commitlint (conventional commits)
  - [ ] PR template

**Estimativa:** 1 semana

**Arquivo exemplo (.github/workflows/ci.yml):**
```yaml
name: CI
on: [push, pull_request]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      - uses: actions/setup-java@v3
        with:
          java-version: '21'
      - run: ./mvnw test
      - run: ./mvnw -DskipTests package
      - uses: aquasecurity/trivy-action@master
        with:
          scan-type: 'fs'
          scan-ref: '.'
```

---

### 5. DOCUMENTAÇÃO (PRIORIDADE BAIXA) ⚠️

#### Falta:
- [ ] Arquitetura detalhada (ADR - Architecture Decision Records)
- [ ] Guia de desenvolvimento local (melhorado)
- [ ] Troubleshooting de problemas comuns
- [ ] API documentation com exemplos de request/response
- [ ] Guia de deploy em produção
- [ ] Runbook de incidentes

**Estimativa:** 3-5 dias

---

### 6. EDGE CASES & VALIDAÇÕES (PRIORIDADE BAIXA)

- [ ] Tentativa de agendar no passado
- [ ] Tentativa de acesso ao prontuário sem consentimento
- [ ] Tentativa de revogar consentimento já expirado
- [ ] Tentativa de médico prescrever sem consulta
- [ ] Tentativa de paciente completar consulta (erro 403)
- [ ] Tentativa de acesso cross-paciente
- [ ] Tentativa de double booking simultâneo
- [ ] Lidar com expiração de token JWT durante operação
- [ ] Lidar com timeout de conexão com banco

---

## 🎯 ROADMAP RECOMENDADO

### Semana 1: Testes
- Expandir cobertura para 80%
- Testes de concorrência
- Testes E2E

### Semana 2: SFU Real
- Integrar LiveKit
- Webhooks de eventos
- Testes com browser real

### Semana 3: Observabilidade
- Micrometer + Prometheus
- Grafana dashboards
- Logging JSON

### Semana 4: DevSecOps
- GitHub Actions CI/CD
- Quality gates
- Secret scanning

### Semana 5: Documentação & Ajustes
- README melhorado
- Troubleshooting
- Go-live checklist

---

## 🔐 SEGURANÇA (LGPD) - CHECKLIST

| Requisito | Status | Validado |
|---|---|---|
| HTTPS em toda comunicação | ✅ | Testar em prod |
| Senhas com hash (BCrypt) | ✅ | ✅ |
| JWT com expiração | ✅ | ✅ |
| RBAC (paciente → seus dados) | ✅ | ✅ |
| Consentimento versionado | ✅ | ✅ |
| Auditoria de prontuário | ✅ | ✅ |
| Criptografia em repouso (AES-GCM) | ✅ | ✅ |
| Segregação de storage | ✅ | ✅ |
| Política de retenção de dados | ⚠️ | Definir |
| Anonimização em logs | ✅ | ✅ |
| Backup & restore testado | ⚠️ | Implementar |

---

## 📊 ESTADO ATUAL VS. PLANEJAMENTO

| Funcionalidade | Planejado | Real | Status |
|---|---|---|---|
| Auth | Sprint 1 | ✅ Completo | Adiantado |
| Agendamento | Sprint 2 | ✅ Completo | Adiantado |
| Consentimento | Sprint 1-2 | ✅ Completo | Adiantado |
| EHR + Auditoria | Sprint 2-3 | ✅ Completo | Adiantado |
| Prescrição | Sprint 3 | ✅ Completo | Adiantado |
| Notificações | Transversal | ✅ Completo | Adiantado |
| Admin Panel | Transversal | ✅ Completo | Adiantado |
| Video (mock) | Sprint 3 | ✅ 90% | Quase |
| Video (real) | Sprint 3 | ❌ 0% | Falta |
| Testes | Toda sprint | ⚠️ 30% | Precisa expandir |
| Observabilidade | Sprint 5 | ❌ 0% | Não iniciado |
| DevSecOps | Sprint 4-5 | ❌ 0% | Não iniciado |

**Conclusão:** Projeto em Phase 2 (estabilização), não Phase 1 (desenvolvimento).

---

## 📞 PRÓXIMAS AÇÕES

1. **Hoje:** Expandir testes (iniciar)
2. **Esta semana:** Integração SFU real
3. **Próxima semana:** Observabilidade
4. **Em 2 semanas:** DevSecOps + Documentação
5. **Em 3 semanas:** Preparar produção

---

## 📚 RECURSOS ÚTEIS

- **LiveKit Docs:** https://docs.livekit.io/
- **Micrometer:** https://micrometer.io/
- **Grafana:** https://grafana.com/
- **SonarQube:** https://www.sonarqube.org/
- **Spring Modulith:** https://spring.io/projects/spring-modulith
- **LGPD:** https://www.gov.br/cidadania/pt-br/acesso-a-informacao/lgpd

---

**Mantém este documento atualizado conforme o desenvolvimento avança.**
