# ── Stage 1: Build ───────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17-alpine AS build
WORKDIR /app

# Copy pom first so dependency layer is cached unless pom changes
COPY pom.xml .
RUN mvn dependency:go-offline -q

COPY src ./src
RUN mvn package -DskipTests -q

# ── Stage 2: Run ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Non-root user for security
RUN addgroup -S audita && adduser -S audita -G audita
USER audita

COPY --from=build /app/target/*.jar app.jar

# JVM flags tuned for 512MB (Render free tier)
ENV JAVA_OPTS="-Xmx380m -Xms128m -Xss512k -XX:+UseSerialGC -XX:MaxMetaspaceSize=96m"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
