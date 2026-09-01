# Vida Conecta — Backend

Monólito modular Spring Boot que concentra a API de negócio da plataforma: autenticação, agendamento, consentimento (LGPD), prontuário cifrado, prescrição digital e emissão de token de videochamada. A mídia WebRTC **não** passa por este serviço.

## Módulos

Pacotes em `br.com.vidaconecta`, fronteiras verificadas com [Spring Modulith](https://docs.spring.io/spring-modulith/reference/):

| Módulo | Pacote | Responsabilidade |
| --- | --- | --- |
| Shared | `shared` | Exceções, `ApiError`, OpenAPI |
| Identity | `identity` | Cadastro, login JWT, papéis (paciente, médico, admin) e Spring Security |
| Scheduling | `scheduling` | Médicos, horários disponíveis, consultas, conflitos, confirmar/cancelar |
| Notification | `notification` | Notificações in-app dos eventos clínicos (confirmação, cancelamento, receita, consentimento) |
| Consent | `consent` | Consentimento versionado (por médico ou por consulta) |
| EHR | `ehr` | Prontuário cifrado (AES-GCM) e auditoria de acesso |
| Prescription | `prescription` | Receita digital ligada à consulta |
| Video | `video` | Token de sala (provider mock; LiveKit depois) |

Identity **é** um módulo: Security é a biblioteca; o bounded context de identidade é quem possui usuários, papéis e JWT. Autorização clínica (quem lê prontuário) fica em Consent + EHR.

## Pré-requisitos

- Java 21
- Maven Wrapper (`./mvnw`)
- Docker (PostgreSQL local e Testcontainers)

## Subir o banco

```bash
docker compose up -d
```

## Executar a API

```bash
./mvnw spring-boot:run
```

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### Variáveis de ambiente

| Variável | Padrão (dev) |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/vida_conecta` |
| `DATABASE_USERNAME` / `DATABASE_PASSWORD` | `vida_conecta` |
| `JWT_SECRET` | chave local de desenvolvimento (≥ 32 caracteres) |
| `EHR_ENCRYPTION_KEY` | Base64 de 32 bytes |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` (aceita padrões, ex. `https://*.vercel.app`) |
| `FRONTEND_BASE_URL` | `http://localhost:5173` (link dos convites; na Vercel use a URL do front) |
| `SES_ENABLED` | `false` (em `true`, envia convite pelo Amazon SES) |
| `MAIL_FROM` | remetente verificado no SES |
| `AWS_REGION` | `us-east-1` |

## Testes

```bash
./mvnw test
```

Os testes de integração sobem PostgreSQL via Testcontainers. `ModularityTests` valida que um módulo só usa a API pública (`api/`) dos outros.

## API (v1)

- `POST /api/v1/auth/register` (paciente) · `POST /api/v1/auth/register/admin` · `POST /api/v1/auth/register/doctor` · `GET /api/v1/auth/invites/{token}` · `POST /api/v1/auth/login` · `GET /api/v1/auth/me`
- `GET /api/v1/admin/bootstrap-token` · `GET|POST /api/v1/admin/doctors/invites` · `GET /api/v1/admin/doctors` · `PATCH /api/v1/admin/doctors/{id}/enabled` · `GET /api/v1/admin/insights`
- `GET /api/v1/doctors` · `GET /api/v1/doctors/{id}/availability` · `GET /api/v1/doctors/{id}/slots`
- `GET|POST /api/v1/me/availability` · `DELETE /api/v1/me/availability/{id}`
- `POST /api/v1/appointments` · `GET /api/v1/appointments` · `GET /api/v1/appointments/{id}` · `POST .../confirm` · `POST .../cancel` · `POST .../complete`
- `GET /api/v1/notifications` · `GET /api/v1/notifications/unread-count` · `POST .../{id}/read` · `POST .../read-all`
- `POST /api/v1/consents` · `GET /api/v1/consents` · `POST /api/v1/consents/{id}/revoke`
- `POST /api/v1/patients/{patientId}/ehr` · `GET /api/v1/patients/{patientId}/ehr` · `GET /api/v1/ehr/audit`
- `POST /api/v1/prescriptions` · `GET /api/v1/prescriptions`
- `POST /api/v1/video/appointments/{id}/token`

Cadastro público (`POST /api/v1/auth/register`) é exclusivo para pacientes.

O primeiro administrador usa o token UUID guardado em `admin_bootstrap_tokens` (seed local: `b2222222-2222-4222-8222-222222222222`) em `POST /api/v1/auth/register/admin`. Depois do uso o token é apagado e um novo é gravado; a resposta devolve `nextBootstrapToken`. Administradores autenticados consultam o token vigente em `GET /api/v1/admin/bootstrap-token`.

Médicos não se cadastram sozinhos: o admin convida com nome e e-mail (`POST /api/v1/admin/doctors/invites`). O convite vai por e-mail (AWS SES quando `SES_ENABLED=true`) com o link `/cadastro/medico?token=...`. O médico conclui em `POST /api/v1/auth/register/doctor`.

O painel do administrador (`GET /api/v1/admin/insights`) devolve totais de consultas, evolução dos últimos 30 dias (fuso `America/Sao_Paulo`), especialidades e o censo do sistema. `PATCH /api/v1/admin/doctors/{id}/enabled` ativa ou desativa o médico: conta desativada some da listagem pública e não consegue entrar.

JWT no header `Authorization: Bearer <token>`.

O médico precisa informar um motivo (mínimo 10 caracteres) ao cancelar. O paciente recebe a notificação com o motivo e um atalho para reagendar.
