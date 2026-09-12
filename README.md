# Vida Conecta — Backend

[![CI](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/ci.yml)
[![CodeQL](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/codeql.yml/badge.svg)](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/codeql.yml)

API de negócio do Vida Conecta, construída como um monólito modular em Spring Boot. O backend concentra autenticação, agendamento, consentimento LGPD, prontuário cifrado, prescrição digital, notificações e emissão de token para a sala de consulta.

> **Status do MVP:** a videochamada já funciona com Jitsi Meet. O backend emite um token mock apenas para autorizar a entrada na sala; a mídia WebRTC não passa por este serviço.

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
| Video | `video` | Autorização da sala Jitsi e emissão do token mock |

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
- `POST /api/v1/video/appointments/{id}/session`

O endpoint de vídeo libera um token mock somente para consulta confirmada e dentro da janela de atendimento. O frontend usa esse resultado para entrar na sala Jitsi; a mídia não passa pelo backend.

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

### Configuração no GitHub

1. Proteja a branch `main` em *Settings → Branches* e exija o check **CI concluída** antes do merge.
2. Habilite o *Code scanning* em *Security → Code scanning* para o CodeQL publicar alertas.
3. Mantenha o *Dependency graph* habilitado em *Settings → Code security* para executar o `dependency-review` nos pull requests.
4. Use títulos de pull request em Conventional Commits, por exemplo: `feat: adiciona cancelamento de consulta`.

A CI não precisa de secrets. O smoke test usa credenciais descartáveis, e o `pull-request.yml` pode pular a revisão de dependências quando o Dependency graph estiver indisponível.

## Entrega contínua

O workflow `cd.yml` constrói uma imagem multi-arquitetura (`linux/amd64` e `linux/arm64`) com Docker Buildx e publica no Docker Hub.

| Gatilho | Publicação |
| --- | --- |
| `workflow_run` (após `CI`) | Toda vez que a CI termina com sucesso em `main` |
| `push` de tag `v*.*.*` | Publica também as tags semânticas (`v1.2.3`, `1.2`) — crie a tag a partir de um commit que já esteja em `main` (e portanto já passou na CI) |
| `workflow_dispatch` | Disparo manual pela aba Actions |

Cada publicação recebe uma tag própria e imutável: `<versão do pom.xml>.<data>.<hora UTC>` (ex.: `1.1.1.20260911.230556`) — sem `latest`. Assim dá pra saber, só pela tag, exatamente qual código e qual build estão rodando em produção, e um redeploy nunca sobrescreve silenciosamente a tag que outra coisa possa estar usando.

A imagem é multi-arquitetura (`linux/amd64` nativo em `ubuntu-latest` + `linux/arm64` nativo em `ubuntu-24.04-arm`, sem QEMU), então a mesma tag roda tanto em EC2 Intel/AMD quanto Graviton. O `Dockerfile` é multi-stage com o JAR extraído em camadas (`-Djarmode=tools extract --layers`) e roda como usuário não-root (`vidaconecta`). Cada arquitetura é escaneada com Trivy antes de publicar — CVE **CRITICAL** com correção disponível barra a publicação daquela perna, do mesmo jeito que o job de segurança da CI barra o SBOM.

Configure no GitHub:

- Variable `DOCKERHUB_USERNAME` com o usuário do Docker Hub.
- Secret `DOCKERHUB_TOKEN` com um Access Token do Docker Hub.

Destino da imagem: `<DOCKERHUB_USERNAME>/vida-conecta-backend`.

### Configurar o Docker Hub

1. Crie um Access Token em *Docker Hub → Account Settings → Security*; não use a senha da conta.
2. No repositório GitHub, cadastre `DOCKERHUB_USERNAME` como **Variable**.
3. Cadastre `DOCKERHUB_TOKEN` como **Secret**.
4. Garanta que o repositório de destino `<DOCKERHUB_USERNAME>/vida-conecta-backend` exista ou permita sua criação no primeiro push.

O workflow publica uma tag imutável por build e também tags semânticas nas releases. O deploy de produção deve usar uma tag real publicada, nunca `latest`.

## Deploy no EC2

Feito manualmente, uma única vez — deploys seguintes são automáticos (ver abaixo). Siga o cabeçalho do próprio `docker-compose.prod.yml`: certificado com certbot, `.env` a partir de `.env.prod.example` (preenchendo `BACKEND_IMAGE` com uma tag real publicada — veja as tags em `hub.docker.com/r/<usuario>/vida-conecta-backend/tags`, nunca `latest`), e `docker compose -f docker-compose.prod.yml up -d`.

### Variáveis de produção

Copie `.env.prod.example` para `.env` no mesmo diretório do Compose e aplique `chmod 600 .env`. Os valores principais são:

| Variável | Finalidade |
| --- | --- |
| `POSTGRES_DB`, `POSTGRES_USER`, `POSTGRES_PASSWORD` | Credenciais do PostgreSQL |
| `JWT_SECRET` | Segredo para assinatura dos tokens JWT |
| `JWT_EXPIRATION_MINUTES` | Tempo de expiração do JWT |
| `EHR_ENCRYPTION_KEY` | Chave Base64 de 32 bytes para cifrar o prontuário |
| `CORS_ALLOWED_ORIGINS` | Origens permitidas para o frontend |
| `FRONTEND_BASE_URL` | URL usada nos links de convite |
| `SES_ENABLED`, `MAIL_FROM`, `AWS_REGION` | Configuração de envio de convites por SES |
| `API_DOMAIN` | Domínio usado pelo Nginx e pelo certificado TLS |
| `BACKEND_IMAGE` | Imagem e tag imutável publicadas no Docker Hub |
| `GRAFANA_PORT`, `GRAFANA_ADMIN_USER`, `GRAFANA_ADMIN_PASSWORD` | Acesso ao Grafana |

Para o primeiro setup, libere as portas 80 e 443 no Security Group, gere o certificado com Certbot e execute:

```bash
sudo certbot certonly --standalone -d api.seudominio.com
cp .env.prod.example .env
chmod 600 .env
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

### Stack de produção

O `docker-compose.prod.yml` executa PostgreSQL, API, Nginx com TLS, Prometheus, Grafana, exporter do PostgreSQL e backup automático. O health check da API fica disponível em `https://<API_DOMAIN>/actuator/health`; o Grafana usa a porta configurada em `GRAFANA_PORT` e deve ser restrito no Security Group.

## Segurança e observações

- Não versionar segredos, chaves AWS, `JWT_SECRET` ou `EHR_ENCRYPTION_KEY`.
- Usar HTTPS em produção.
- O acesso ao prontuário depende de autenticação, papéis e consentimento válido.
- O prontuário usa criptografia AES-GCM e possui auditoria de acessos.
- O banco transacional e o storage clínico devem usar criptografia em repouso e backups.
- A mídia WebRTC do Jitsi não é armazenada pelo backend.
- O primeiro administrador usa um token UUID de bootstrap armazenado em `admin_bootstrap_tokens`; após o uso, o token é substituído.

### Deploy automático após a publicação

Depois do `publish`, o `cd.yml` roda um job `deploy` que atualiza o serviço `api` no EC2 — via **AWS Systems Manager (SSM) Run Command**, sem SSH exposto à internet e sem chave privada guardada em Secret. O GitHub Actions assume uma IAM role via OIDC (nenhuma credencial de longa duração fica no repositório).

O job faz duas coisas na instância, nessa ordem:

1. Escreve o `docker-compose.prod.yml` do commit publicado no diretório da instância — o arquivo na EC2 nunca fica desatualizado em relação ao que está em `main`.
2. Roda `deploy/ec2-deploy.sh`, que faz `docker compose pull api && docker compose up -d api`.

Como o Compose só recria um serviço quando a configuração dele muda, **Postgres, Nginx e a stack de observabilidade (Prometheus, Grafana, exporter, backup) nunca são tocados** por um deploy — só a imagem da API muda entre uma execução e outra. O `.env` da instância (com os segredos reais) nunca é sobrescrito por esse processo: **nenhum segredo passa pelo GitHub Actions nem pelo histórico do SSM**.

Pré-requisitos na conta AWS (feitos uma vez):

1. **IAM role na instância EC2**, com a policy gerenciada `AmazonSSMManagedInstanceCore` (para o SSM Agent se registrar) — anexada em *EC2 → instância → Security → Modify IAM role*.
2. **Provedor OIDC do GitHub** (`token.actions.githubusercontent.com`) configurado em *IAM → Identity providers*.
3. **Uma IAM role assumível pelo GitHub Actions**, com trust policy restrita a este repositório e branch (`repo:iagobcosta/vida-conecta-backend:ref:refs/heads/main`) e permissão de `ssm:SendCommand` restrita à instância específica, mais `ssm:GetCommandInvocation`/`ssm:ListCommandInvocations`.
4. **`docker-compose.prod.yml`, `.env` (a partir de `.env.prod.example`) e as pastas `nginx/`/`observability/` já colocadas manualmente no diretório da instância** — o pipeline só mantém o `docker-compose.prod.yml` sincronizado a cada deploy, o resto é setup único.

Depois disso, quatro **Variables** no repositório (*Settings → Secrets and variables → Actions → Variables* — nenhuma é segredo, a ARN da role não concede nada sozinha, quem autoriza é a trust policy):

| Variable | Exemplo |
| --- | --- |
| `AWS_ROLE_ARN` | `arn:aws:iam::<account-id>:role/<nome-da-role>` |
| `AWS_REGION` | `us-east-2` |
| `EC2_INSTANCE_ID` | `i-xxxxxxxxxxxxxxxxx` |
| `EC2_COMPOSE_DIR` | `/home/ubuntu` — diretório na instância onde ficam o `docker-compose.prod.yml`, o `.env` e as pastas `nginx/`/`observability/` |

Para reproduzir o script de deploy localmente (contra um `docker-compose.prod.yml` de teste, não a instância real):

```bash
DEPLOY_IMAGE=<imagem>:<tag> DEPLOY_COMPOSE_DIR=<diretório-com-o-compose-e-o-.env> bash deploy/ec2-deploy.sh
```
