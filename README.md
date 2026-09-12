# Vida Conecta — Backend

[![CI](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/ci.yml)
[![CodeQL](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/codeql.yml/badge.svg)](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/codeql.yml)

API de negócio do Vida Conecta, construída como um monólito modular em Spring Boot. O backend concentra autenticação, agendamento, consentimento LGPD, prontuário cifrado, prescrição digital, notificações e emissão de token para a sala de consulta.

> **Status do MVP:** o token da sala de vídeo ainda é mock. A integração com WebRTC/SFU, como LiveKit, será realizada em uma etapa futura. A mídia da chamada não passa pelo backend.

## Índice

- [Módulos](#módulos)
- [Pré-requisitos](#pré-requisitos)
- [Execução local](#execução-local)
- [Configuração](#configuração)
- [API v1](#api-v1)
- [Testes](#testes)
- [CI e qualidade](#ci-e-qualidade)
- [Entrega contínua](#entrega-contínua)
- [Deploy no EC2](#deploy-no-ec2)
- [Segurança e observações](#segurança-e-observações)

## Módulos

Os módulos ficam em `br.com.vidaconecta`. Suas fronteiras são verificadas com [Spring Modulith](https://docs.spring.io/spring-modulith/reference/).

| Módulo | Pacote | Responsabilidade |
| --- | --- | --- |
| Shared | `shared` | Exceções, `ApiError` e OpenAPI |
| Identity | `identity` | Cadastro, login JWT, papéis e Spring Security |
| Scheduling | `scheduling` | Médicos, disponibilidades, consultas e conflitos de agenda |
| Notification | `notification` | Notificações in-app de eventos clínicos |
| Consent | `consent` | Consentimento versionado por médico ou consulta |
| EHR | `ehr` | Prontuário cifrado com AES-GCM e auditoria de acesso |
| Prescription | `prescription` | Receita digital vinculada à consulta |
| Portability | `portability` | Exportação de dados conforme a LGPD |
| Video | `video` | Emissão do token mock da sala de consulta |

O módulo `Identity` é responsável por usuários, papéis e JWT. A autorização clínica, ou seja, quem pode ler o prontuário, é controlada pelos módulos `Consent` e `EHR`.

## Pré-requisitos

- Java 21
- Maven Wrapper (`mvnw.cmd` no Windows ou `./mvnw` no Linux/macOS)
- Docker, para PostgreSQL local e Testcontainers

## Execução local

### 1. Subir o banco

```bash
docker compose up -d
```

### 2. Iniciar a API

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Linux/macOS:

```bash
./mvnw spring-boot:run
```

Endpoints locais:

- API: http://localhost:8080
- Swagger UI: http://localhost:8080/swagger-ui.html
- Health check: http://localhost:8080/actuator/health

## Configuração

As configurações podem ser fornecidas por variáveis de ambiente. Os valores abaixo são adequados para desenvolvimento local.

| Variável | Padrão ou finalidade |
| --- | --- |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/vida_conecta` |
| `DATABASE_USERNAME` | `vida_conecta` |
| `DATABASE_PASSWORD` | Senha do banco local |
| `JWT_SECRET` | Segredo local com pelo menos 32 caracteres |
| `EHR_ENCRYPTION_KEY` | Chave Base64 de 32 bytes para o prontuário |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173` |
| `FRONTEND_BASE_URL` | `http://localhost:5173`, usado nos links de convite |
| `SES_ENABLED` | `false`; quando `true`, envia convites pelo Amazon SES |
| `MAIL_FROM` | Remetente verificado no SES |
| `AWS_REGION` | `us-east-1` |
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | Credenciais IAM com `ses:SendEmail`; nunca versionar |

O envio de e-mail implementado é usado para convites de médicos feitos pelo administrador. A validação de e-mail no cadastro ainda não faz parte do MVP.

### Convite de médicos

Médicos não se cadastram sozinhos. O administrador envia um convite com nome e e-mail; o médico conclui o cadastro pelo link recebido. Com `SES_ENABLED=false`, o link aparece no log da API e no painel administrativo. Com `SES_ENABLED=true`, o envio é feito pelo Amazon SES.

Não grave Access Keys no YAML. Em desenvolvimento, o `SesClient` pode usar as credenciais configuradas pelo AWS CLI:

```bash
aws configure
```

## API v1

Todas as rotas protegidas usam o header:

```text
Authorization: Bearer <token>
```

### Autenticação e conta

- `POST /api/v1/auth/register` — cadastro de paciente
- `POST /api/v1/auth/register/admin` — cadastro inicial de administrador
- `POST /api/v1/auth/register/doctor` — conclusão do cadastro por convite
- `GET /api/v1/auth/invites/{token}` — consulta de convite
- `POST /api/v1/auth/login` — login
- `GET /api/v1/auth/me` — usuário autenticado
- `PATCH /api/v1/auth/me` — atualização de dados
- `DELETE /api/v1/auth/me` — exclusão/anonimização da conta
- `GET /api/v1/auth/me/export` — exportação dos dados do titular

### Administração

- `GET /api/v1/admin/bootstrap-token`
- `GET|POST /api/v1/admin/doctors/invites`
- `GET /api/v1/admin/doctors`
- `PATCH /api/v1/admin/doctors/{id}/enabled`
- `GET /api/v1/admin/insights`

O painel administrativo apresenta totais de consultas, evolução dos últimos 30 dias, especialidades e censo do sistema. Médicos desativados deixam de aparecer na listagem pública e não conseguem acessar a plataforma.

### Agendamento

- `GET /api/v1/doctors`
- `GET /api/v1/doctors/{id}/availability`
- `GET /api/v1/doctors/{id}/slots`
- `GET|POST /api/v1/me/availability`
- `DELETE /api/v1/me/availability/{id}`
- `POST /api/v1/appointments`
- `GET /api/v1/appointments`
- `GET /api/v1/appointments/{id}`
- `POST /api/v1/appointments/{id}/confirm`
- `POST /api/v1/appointments/{id}/cancel`
- `POST /api/v1/appointments/{id}/complete`

Ao cancelar, o médico deve informar um motivo com pelo menos 10 caracteres. O paciente recebe uma notificação e pode reagendar.

### Notificações, consentimento e prontuário

- `GET /api/v1/notifications`
- `GET /api/v1/notifications/unread-count`
- `POST /api/v1/notifications/{id}/read`
- `POST /api/v1/notifications/read-all`
- `POST /api/v1/consents`
- `GET /api/v1/consents`
- `POST /api/v1/consents/{id}/revoke`
- `POST /api/v1/patients/{patientId}/ehr`
- `GET /api/v1/patients/{patientId}/ehr`
- `GET /api/v1/ehr/audit`

O médico só acessa o histórico clínico quando existe consentimento válido. O prontuário é cifrado e os acessos são auditados.

### Prescrição e vídeo

- `POST /api/v1/prescriptions`
- `GET /api/v1/prescriptions`
- `GET /api/v1/prescriptions/{id}`
- `POST /api/v1/video/appointments/{id}/token`

O endpoint de vídeo libera um token mock somente para consulta confirmada e dentro da janela de atendimento. Uma integração real com LiveKit/SFU será adicionada posteriormente.

## Testes

Windows:

```powershell
.\mvnw.cmd test
```

Linux/macOS:

```bash
./mvnw test
```

Os testes de integração usam PostgreSQL via Testcontainers. `ModularityTests` verifica se os módulos respeitam suas APIs públicas.

Para executar a verificação com o mesmo limite de cobertura da CI:

```bash
./mvnw verify -Djacoco.line.minimum=0.80
```

O relatório de cobertura fica em `target/site/jacoco/index.html`.

## CI e qualidade

A CI principal é executada pelo GitHub Actions e valida:

| Verificação | Objetivo |
| --- | --- |
| Build e empacotamento | Compilar o projeto, gerar JAR executável e SBOM |
| Arquitetura modular | Executar `ModularityTests` sem Docker |
| Testes e cobertura | Executar a suíte com Testcontainers e JaCoCo |
| Smoke test | Subir o JAR contra PostgreSQL e validar endpoints básicos |
| Segurança | Executar Gitleaks, Trivy e CodeQL |
| Qualidade | Validar workflows, migrations e dependências |
| Imagem | Construir a imagem de container em `main` |

Workflows auxiliares:

- `codeql.yml`: análise SAST do código Java.
- `pull-request.yml`: Conventional Commits e análise de dependências.
- `dependabot.yml`: atualizações semanais de dependências.

Comandos úteis para validação local:

```bash
./mvnw verify -Djacoco.line.minimum=0.80
./mvnw test -Dtest=ModularityTests
bash .github/scripts/check-migrations.sh
```

## Entrega contínua

O workflow `cd.yml` constrói uma imagem multi-arquitetura (`linux/amd64` e `linux/arm64`) com Docker Buildx e publica no Docker Hub.

| Gatilho | Publicação |
| --- | --- |
| `workflow_run` após a CI em `main` | `latest` e SHA curto |
| Tag `v*.*.*` | Tags semânticas, como `v1.2.3` e `1.2` |
| `workflow_dispatch` | Publicação manual |

Configure no GitHub:

- Variable `DOCKERHUB_USERNAME` com o usuário do Docker Hub.
- Secret `DOCKERHUB_TOKEN` com um Access Token do Docker Hub.

Destino da imagem: `<DOCKERHUB_USERNAME>/vida-conecta-backend`.

## Deploy no EC2

Exemplo de execução da imagem publicada:

```bash
docker pull <usuario>/vida-conecta-backend:latest

docker run -d --name vida-conecta-api --restart unless-stopped -p 8080:8080 \
  -e DATABASE_URL=jdbc:postgresql://<host-do-postgres>:5432/vida_conecta \
  -e DATABASE_USERNAME=vida_conecta \
  -e DATABASE_PASSWORD=<senha> \
  -e JWT_SECRET=<segredo-de-producao> \
  -e EHR_ENCRYPTION_KEY=<chave-base64-32-bytes> \
  -e SES_ENABLED=true \
  -e MAIL_FROM=<remetente-verificado> \
  -e AWS_REGION=us-east-1 \
  -e CORS_ALLOWED_ORIGINS=https://seu-frontend.exemplo \
  -e FRONTEND_BASE_URL=https://seu-frontend.exemplo \
  <usuario>/vida-conecta-backend:latest
```

O `Dockerfile` usa build multi-stage e executa a aplicação com o usuário não-root `vidaconecta`. A publicação da imagem não automatiza o restart no EC2; essa etapa pode ser feita posteriormente via SSM ou SSH.

## Segurança e observações

- Não versionar segredos, chaves AWS, `JWT_SECRET` ou `EHR_ENCRYPTION_KEY`.
- Usar HTTPS em produção.
- O acesso ao prontuário depende de autenticação, papéis e consentimento válido.
- O prontuário usa criptografia AES-GCM e possui auditoria de acessos.
- O banco transacional e o storage clínico devem usar criptografia em repouso e backups.
- A mídia WebRTC não é armazenada pelo backend.
- O primeiro administrador usa um token UUID de bootstrap armazenado em `admin_bootstrap_tokens`; após o uso, o token é substituído.
