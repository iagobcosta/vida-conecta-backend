# Vida Conecta — Backend

[![CI](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/ci.yml/badge.svg)](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/ci.yml)
[![CodeQL](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/codeql.yml/badge.svg)](https://github.com/iagobcosta/vida-conecta-backend/actions/workflows/codeql.yml)

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
| Portability | `portability` | Exportação de dados (LGPD) |
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
| `AWS_ACCESS_KEY_ID` / `AWS_SECRET_ACCESS_KEY` | credenciais IAM com permissão `ses:SendEmail` (não versionar) |

## Testes

```bash
./mvnw test
```

Os testes de integração sobem PostgreSQL via Testcontainers. `ModularityTests` valida que um módulo só usa a API pública (`api/`) dos outros.

Para rodar a suíte com o mesmo portão de cobertura da CI:

```bash
./mvnw verify -Djacoco.line.minimum=0.80
```

O relatório fica em `target/site/jacoco/index.html`.

## Integração contínua

Toda a CI roda no GitHub Actions. `ci.yml` é a pipeline principal; os outros workflows cuidam de análise de segurança e das regras de pull request.

| Job | O que valida | Reprova quando |
| --- | --- | --- |
| **Build e empacotamento** | POM válido, compilação de `main` e `test`, JAR executável e SBOM (CycloneDX) | Erro de compilação ou de empacotamento |
| **Arquitetura modular** | `ModularityTests` isolado, sem Docker — feedback em ~1 min | Um módulo acessa algo fora do `api/` de outro |
| **Testes e cobertura** | Suíte completa com Testcontainers + `jacoco:check` | Teste vermelho ou cobertura de linhas abaixo de 80% |
| **Smoke test** | JAR real contra PostgreSQL real: Flyway na base limpa, boot com config de produção, `/v3/api-docs` e 401 em rota protegida | App não sobe, migration falha, contrato vazio ou rota protegida sem autenticação |
| **Segurança** | `gitleaks` no histórico do Git e `trivy` sobre o SBOM | Segredo versionado ou CVE **CRITICAL** com correção disponível |
| **Qualidade e convenções** | `actionlint` nos workflows, padrão de nome das migrations, imutabilidade das migrations já mergeadas, relatório de dependências | Workflow inválido, migration fora do padrão ou alteração de `V*.sql` já mergeado |
| **Imagem de container** | `spring-boot:build-image` (buildpacks) — só em `main` | A imagem não constrói |
| **CI concluída** | Agrega o resultado de todos os jobs | Qualquer job acima falhou |

Workflows auxiliares:

- **`codeql.yml`** — SAST do código Java (`security-and-quality`), em push, PR e semanalmente. Os alertas aparecem em *Security → Code scanning*.
- **`pull-request.yml`** — título do PR em Conventional Commits (o merge é por squash) e `dependency-review` barrando CVE alta ou licença incompatível entrando junto com a mudança.
- **`dependabot.yml`** — PRs semanais de atualização de dependências Maven e das próprias actions, agrupados por ecossistema.

### Reproduzindo os portões localmente

```bash
./mvnw verify -Djacoco.line.minimum=0.80          # testes + cobertura
./mvnw test -Dtest=ModularityTests                # fronteiras entre módulos
bash .github/scripts/check-migrations.sh          # convenção das migrations
docker run --rm -v "$PWD":/repo -w /repo rhysd/actionlint:latest          # workflows
docker run --rm -v "$PWD:/repo" ghcr.io/gitleaks/gitleaks:latest \
  git /repo --config /repo/.gitleaks.toml --redact --no-banner            # segredos
docker run --rm -v "$PWD:/src" aquasec/trivy:latest \
  sbom --severity HIGH,CRITICAL /src/target/bom.json                      # CVEs (após package)
```

### Configuração no GitHub

1. Em *Settings → Branches*, proteja `main` exigindo o check **`CI concluída`** — ele já cobre todos os outros jobs.
2. *Security → Code scanning* precisa estar habilitado para o CodeQL publicar alertas (automático em repositório público).
3. `dependency-review` depende do *Dependency graph* ligado em *Settings → Code security* (quando estiver desligado, o workflow registra aviso e pula essa checagem).
4. Nenhum secret é necessário para a CI: o smoke test usa credenciais descartáveis definidas no próprio workflow. A CD (publicação da imagem) precisa das credenciais descritas abaixo.

## Entrega contínua (CD) — imagem de container

`cd.yml` builda a imagem com Docker Buildx e publica no Docker Hub. Ele **não** roda em pull request — só depois que `ci.yml` passa em `main`, em uma tag de versão, ou sob demanda:

| Gatilho | Quando dispara |
| --- | --- |
| `workflow_run` (após `CI`) | Toda vez que a CI termina com sucesso em `main` |
| `push` de tag `v*.*.*` | Publica também as tags semânticas (`v1.2.3`, `1.2`) — crie a tag a partir de um commit que já esteja em `main` (e portanto já passou na CI) |
| `workflow_dispatch` | Disparo manual pela aba Actions |

Cada publicação recebe uma tag própria e imutável: `<versão do pom.xml>.<data>.<hora UTC>` (ex.: `1.1.1.20260911.230556`) — sem `latest`. Assim dá pra saber, só pela tag, exatamente qual código e qual build estão rodando em produção, e um redeploy nunca sobrescreve silenciosamente a tag que outra coisa possa estar usando.

A imagem é multi-arquitetura (`linux/amd64` nativo em `ubuntu-latest` + `linux/arm64` nativo em `ubuntu-24.04-arm`, sem QEMU), então a mesma tag roda tanto em EC2 Intel/AMD quanto Graviton. O `Dockerfile` é multi-stage com o JAR extraído em camadas (`-Djarmode=tools extract --layers`) e roda como usuário não-root (`vidaconecta`). Cada arquitetura é escaneada com Trivy antes de publicar — CVE **CRITICAL** com correção disponível barra a publicação daquela perna, do mesmo jeito que o job de segurança da CI barra o SBOM.

### Configurar o Docker Hub

1. Crie um [Access Token](https://hub.docker.com/settings/security) no Docker Hub (não use a senha da conta).
2. Em *Settings → Secrets and variables → Actions* do repositório:
   - **Variables** → `DOCKERHUB_USERNAME` = seu usuário do Docker Hub (não é segredo, só identifica o repositório da imagem).
   - **Secrets** → `DOCKERHUB_TOKEN` = o Access Token gerado.
3. Repositório de destino: `<DOCKERHUB_USERNAME>/vida-conecta-backend` — o Docker Hub cria o repositório automaticamente no primeiro push (fica público por padrão; torne privado nas configurações do repositório se necessário).

### Subindo pela primeira vez no EC2

Feito manualmente, uma única vez — deploys seguintes são automáticos (ver abaixo). Siga o cabeçalho do próprio `docker-compose.prod.yml`: certificado com certbot, `.env` a partir de `.env.prod.example` (preenchendo `BACKEND_IMAGE` com uma tag real publicada — veja as tags em `hub.docker.com/r/<usuario>/vida-conecta-backend/tags`, nunca `latest`), e `docker compose -f docker-compose.prod.yml up -d`.

### Deploy automático a cada publicação

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

## Produção — stack completa (`docker-compose.prod.yml`)

Postgres + API + Nginx (TLS) + Prometheus + Grafana + backup automático do banco. Ver o cabeçalho do próprio arquivo para o passo a passo de setup inicial na instância (certificado com certbot, `.env` a partir de `.env.prod.example`, `docker compose -f docker-compose.prod.yml up -d`). Depois do setup inicial, deploys de uma nova versão da API acontecem sozinhos via `cd.yml` — não é preciso repetir esse passo manualmente.

## API (v1)

- `POST /api/v1/auth/register` (paciente) · `POST /api/v1/auth/register/admin` · `POST /api/v1/auth/register/doctor` · `GET /api/v1/auth/invites/{token}` · `POST /api/v1/auth/login` · `GET /api/v1/auth/me` · `DELETE /api/v1/auth/me` · `PATCH /api/v1/auth/me` · `GET /api/v1/auth/me/export`
- `GET /api/v1/admin/bootstrap-token` · `GET|POST /api/v1/admin/doctors/invites` · `GET /api/v1/admin/doctors` · `PATCH /api/v1/admin/doctors/{id}/enabled` · `GET /api/v1/admin/insights`
- `GET /api/v1/doctors` · `GET /api/v1/doctors/{id}/availability` · `GET /api/v1/doctors/{id}/slots`
- `GET|POST /api/v1/me/availability` · `DELETE /api/v1/me/availability/{id}`
- `POST /api/v1/appointments` · `GET /api/v1/appointments` · `GET /api/v1/appointments/{id}` · `POST .../confirm` · `POST .../cancel` · `POST .../complete`
- `GET /api/v1/notifications` · `GET /api/v1/notifications/unread-count` · `POST .../{id}/read` · `POST .../read-all`
- `POST /api/v1/consents` · `GET /api/v1/consents` · `POST /api/v1/consents/{id}/revoke`
- `POST /api/v1/patients/{patientId}/ehr` · `GET /api/v1/patients/{patientId}/ehr` · `GET /api/v1/ehr/audit`
- `POST /api/v1/prescriptions` · `GET /api/v1/prescriptions`
- `POST /api/v1/video/appointments/{id}/token` · `POST /api/v1/video/appointments/{id}/session`

Cadastro público (`POST /api/v1/auth/register`) é exclusivo para pacientes.

O primeiro administrador usa o token UUID guardado em `admin_bootstrap_tokens` (seed local: `b2222222-2222-4222-8222-222222222222`) em `POST /api/v1/auth/register/admin`. Depois do uso o token é apagado e um novo é gravado; a resposta devolve `nextBootstrapToken`. Administradores autenticados consultam o token vigente em `GET /api/v1/admin/bootstrap-token`.

Médicos não se cadastram sozinhos: o admin convida com nome e e-mail (`POST /api/v1/admin/doctors/invites`). O convite vai por e-mail (AWS SES quando `SES_ENABLED=true`) com o link `/cadastro/medico?token=...`. O médico conclui em `POST /api/v1/auth/register/doctor`.

Para o SES, **não** grave Access Key no YAML.

**Na sua máquina, sem e-mail real:** deixe `SES_ENABLED` em `false` (padrão). O convite aparece no log da API e o admin vê o link na tela.

**Na sua máquina, com SES de verdade:**

1. No console AWS, verifique o remetente (`MAIL_FROM`). Em sandbox, verifique também o e-mail do médico de teste.
2. Grave as credenciais uma vez (não precisa exportar a cada execução):

```bash
aws configure
```

Isso cria `~/.aws/credentials`. O `SesClient` lê esse arquivo automaticamente.

3. Suba a API com o SES ligado:

```bash
export SES_ENABLED=true
export MAIL_FROM=seu-email-verificado@seudominio.com
export AWS_REGION=us-east-1
./mvnw spring-boot:run
```

Se não usar o AWS CLI, exporte também `AWS_ACCESS_KEY_ID` e `AWS_SECRET_ACCESS_KEY` no mesmo terminal. Em EC2/ECS/Lambda as chaves podem ficar na role IAM.

O painel do administrador (`GET /api/v1/admin/insights`) devolve totais de consultas, evolução dos últimos 30 dias (fuso `America/Sao_Paulo`), especialidades e o censo do sistema. `PATCH /api/v1/admin/doctors/{id}/enabled` ativa ou desativa o médico: conta desativada some da listagem pública e não consegue entrar.

JWT no header `Authorization: Bearer <token>`.

O médico precisa informar um motivo (mínimo 10 caracteres) ao cancelar. O paciente recebe a notificação com o motivo e um atalho para reagendar.
