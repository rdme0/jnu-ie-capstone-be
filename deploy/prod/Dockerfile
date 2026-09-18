FROM eclipse-temurin:21-jdk AS build

WORKDIR /workspace

COPY gradlew settings.gradle build.gradle ./
COPY gradle ./gradle

RUN chmod +x ./gradlew

COPY src ./src

RUN ./gradlew --no-daemon bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /workspace/build/libs/*.jar app.jar

EXPOSE 18081

ENTRYPOINT ["java", "-jar", "app.jar"]
