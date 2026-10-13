# Etapa 1: compila o jar com o Maven Wrapper
FROM eclipse-temurin:25-jdk AS build
WORKDIR /app
COPY .mvn .mvn
COPY mvnw pom.xml ./
RUN ./mvnw -q -B dependency:go-offline
COPY src src
RUN ./mvnw -q -B package -DskipTests

# Etapa 2: imagem final só com o JRE
FROM eclipse-temurin:25-jre
WORKDIR /app
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
RUN useradd --system --create-home clinics && mkdir -p /app/data/avatars && chown -R clinics /app
COPY --from=build /app/target/*.jar app.jar
USER clinics
ENV AVATARS_DIR=/app/data/avatars
EXPOSE 8080
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]
