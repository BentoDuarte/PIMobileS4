FROM eclipse-temurin:17-jdk-jammy AS build
WORKDIR /build

COPY gradlew ./
COPY gradle/wrapper/ gradle/wrapper/
COPY api-email/build.gradle api-email/settings.gradle api-email/
COPY api-email/src/ api-email/src/

RUN sh ./gradlew --no-daemon -p api-email test bootJar \
    && cp api-email/build/libs/*.jar /build/api-email.jar

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app

COPY --from=build /build/api-email.jar ./api-email.jar
ENV API_ENDERECO=0.0.0.0
EXPOSE 8080
USER 10001:10001

ENTRYPOINT ["java", "-jar", "/app/api-email.jar"]
