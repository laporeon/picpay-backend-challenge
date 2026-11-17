FROM maven:3.9.11-eclipse-temurin-21-alpine AS build

WORKDIR /app

COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B package -DskipTests

FROM eclipse-temurin:21-jre-alpine-3.21

WORKDIR /app

RUN addgroup -S picpaychallenge \
 && adduser -S picpaychallenge -G picpaychallenge

COPY --from=build --chown=picpaychallenge:picpaychallenge /app/target/*.jar app.jar

USER picpaychallenge

EXPOSE 8080

ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75", "-jar", "app.jar"]