# syntax=docker/dockerfile:1

# ---------------------------------------------------------------------------
# EMSafe Backend — imagen de producción (multi-stage, compatible arm64/amd64)
#
# Etapa 1 (build):   compila el JAR con Maven sobre JDK 17.
# Etapa 2 (runtime): solo el JRE + el JAR. Imagen final ~230 MB en vez de ~800 MB.
#
# El build se cachea por capas: si solo cambia el código fuente (no el pom.xml),
# Docker reutiliza las dependencias ya descargadas y la compilación baja de
# varios minutos a segundos. Importante en una VM ARM de 2 OCPU.
# ---------------------------------------------------------------------------

FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build

# 1) Solo el pom: esta capa se invalida únicamente si cambian las dependencias.
COPY pom.xml .
RUN mvn -B dependency:go-offline

# 2) Ahora sí el código fuente.
COPY src ./src
RUN mvn -B clean package -DskipTests \
    && mv target/emsafe-backend-*.jar target/app.jar


FROM eclipse-temurin:17-jre AS runtime

# Usuario sin privilegios: si alguien escapa de la aplicación, no cae como root.
RUN groupadd --system --gid 1001 emsafe \
    && useradd --system --uid 1001 --gid emsafe --home /app emsafe

WORKDIR /app
COPY --from=build --chown=emsafe:emsafe /build/target/app.jar ./app.jar

USER emsafe

ENV SPRING_PROFILES_ACTIVE=prod \
    SERVER_PORT=8080 \
    JAVA_OPTS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:+ExitOnOutOfMemoryError"

EXPOSE 8080

# Healthcheck sin curl ni wget: /dev/tcp es una función nativa de bash.
# Le damos 90s de gracia porque Spring Boot + Flyway tardan en el primer arranque.
HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD bash -c 'cat < /dev/null > /dev/tcp/127.0.0.1/8080' || exit 1

# 'exec' para que la JVM sea PID 1 y reciba SIGTERM => apagado limpio de Hikari.
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar /app/app.jar"]
