# syntax=docker/dockerfile:1

# Buildado via `docker buildx build --platform linux/amd64,linux/arm64` — roda em
# EC2 Intel/AMD e Graviton com a mesma tag. O host de build pode ser Mac, Linux
# ou Windows (Buildx cuida da emulação via QEMU quando a arquitetura difere).

# --- build: compila o JAR e separa em camadas ------------------------------
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /build

# Copia só o necessário para resolver dependências antes do código-fonte, para
# o cache de camadas do Docker sobreviver a mudanças que não tocam no pom.xml.
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	./mvnw -B -ntp dependency:go-offline

COPY src/ src/
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
	./mvnw -B -ntp package -DskipTests \
	&& java -Djarmode=tools -jar target/*.jar extract --layers --launcher --destination /build/extracted

# --- runtime: só o JRE e as camadas extraídas -------------------------------
FROM eclipse-temurin:21-jre-alpine AS runtime

RUN addgroup -S vidaconecta && adduser -S vidaconecta -G vidaconecta
WORKDIR /app

# Camadas na ordem menos → mais mutável: cada uma vira uma layer OCI separada,
# então trocar só o código da aplicação não invalida o cache das dependências.
COPY --from=build /build/extracted/dependencies/ ./
COPY --from=build /build/extracted/spring-boot-loader/ ./
COPY --from=build /build/extracted/snapshot-dependencies/ ./
COPY --from=build /build/extracted/application/ ./

USER vidaconecta:vidaconecta
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=45s --retries=3 \
	CMD wget -qO- http://localhost:8080/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
