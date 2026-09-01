# Vida Conecta — Backend

Monólito modular Spring Boot que concentra a API de negócio da plataforma: autenticação, agendamento, consentimento (LGPD), prontuário cifrado, prescrição digital e emissão de token de videochamada. A mídia WebRTC **não** passa por este serviço.

## Módulos

Pacotes em `br.com.vidaconecta`, fronteiras verificadas com [Spring Modulith](https://docs.spring.io/spring-modulith/reference/):

| Módulo | Pacote | Responsabilidade |
| --- | --- | --- |
| Shared | `shared` | Exceções, `ApiError`, OpenAPI |
| Identity | `identity` | Cadastro, login JWT, papéis (paciente, médico, admin) e Spring Security |
| Scheduling | `scheduling` | Médicos, horários disponíveis, consultas, conflitos, confirmar/cancelar |
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
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |

## Testes

```bash
./mvnw test
```

Os testes de integração sobem PostgreSQL via Testcontainers. `ModularityTests` valida que um módulo só usa a API pública (`api/`) dos outros.

## API (v1)

- `POST /api/v1/auth/register` · `POST /api/v1/auth/login` · `GET /api/v1/auth/me`
- `GET /api/v1/doctors` · `GET /api/v1/doctors/{id}/availability` · `GET /api/v1/doctors/{id}/slots`
- `GET|POST /api/v1/me/availability` · `DELETE /api/v1/me/availability/{id}`
- `POST /api/v1/appointments` · `GET /api/v1/appointments` · `POST .../confirm` · `POST .../cancel` · `POST .../complete`
- `POST /api/v1/consents` · `GET /api/v1/consents` · `POST /api/v1/consents/{id}/revoke`
- `POST /api/v1/patients/{patientId}/ehr` · `GET /api/v1/patients/{patientId}/ehr` · `GET /api/v1/ehr/audit`
- `POST /api/v1/prescriptions` · `GET /api/v1/prescriptions`
- `POST /api/v1/video/appointments/{id}/token`

Cadastro de médico e paciente usa o mesmo `register`, com `role` `MEDICO` ou `PACIENTE`. JWT no header `Authorization: Bearer <token>`.
