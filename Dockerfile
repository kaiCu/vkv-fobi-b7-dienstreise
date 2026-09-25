# syntax=docker/dockerfile:1
# Stage 1: build the layered jar
FROM eclipse-temurin:25.0.4.1_1-jdk AS build
WORKDIR /workspace
COPY mvnw pom.xml ./
COPY .mvn .mvn
RUN ./mvnw -B -q dependency:go-offline
COPY src src
RUN ./mvnw -B -q package -DskipTests -Dspotless.check.skip=true \
 && cp target/dienstreise-*.jar app.jar \
 && java -Djarmode=tools -jar app.jar extract --layers --launcher --destination extracted

# Stage 2: minimal runtime, non-root, works with a read-only root filesystem
FROM eclipse-temurin:25.0.4.1_1-jre
RUN groupadd --system --gid 10001 app && useradd --system --uid 10001 --gid app --no-create-home app
WORKDIR /app
COPY --from=build /workspace/extracted/dependencies/ ./
COPY --from=build /workspace/extracted/spring-boot-loader/ ./
COPY --from=build /workspace/extracted/snapshot-dependencies/ ./
COPY --from=build /workspace/extracted/application/ ./
ENV TZ=UTC \
    JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -Djava.io.tmpdir=/tmp"
EXPOSE 8080 8081
USER 10001
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
