FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x ./gradlew
RUN ./gradlew clean bootJar -x test

FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/build/libs/*SNAPSHOT.jar app.jar

EXPOSE 10000

CMD ["sh", "-c", "java -XX:MaxRAMPercentage=70.0 -Dserver.address=0.0.0.0 -Dserver.port=${PORT:-10000} -jar app.jar"]
