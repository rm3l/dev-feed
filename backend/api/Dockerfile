FROM openjdk:8-jdk AS builder

ADD . /code/awesome_dev/backend
WORKDIR  /code/awesome_dev/backend

RUN ./gradlew clean build

FROM java:8
COPY --from=builder /code/awesome_dev/backend/build/libs/api-0.0.1-SNAPSHOT.jar /app.jar

CMD ["java", "-jar", "/app.jar"]
