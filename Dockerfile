# Multi-stage Dockerfile for EditShare on Render
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /app
COPY pom.xml mvnw ./
COPY .mvn .mvn
RUN chmod +x mvnw
RUN ./mvnw dependency:go-offline -B || true
COPY src ./src
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
ENV PORT=8085
EXPOSE 8085
COPY --from=build /app/target/editshare-*.jar app.jar

# Uses Render's $PORT dynamically with container memory optimization
ENTRYPOINT ["sh", "-c", "java -XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -Dserver.port=${PORT:-8085} -jar app.jar"]
