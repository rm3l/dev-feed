#FROM openjdk:8-jdk AS builder
#
#ADD . /code/awesome_dev/backend
#WORKDIR  /code/awesome_dev/backend
#
#RUN ./gradlew clean build

FROM java:8

ARG JOB_TOKEN
ARG BACKEND_API_BUILD_JOB_NAME

RUN apt-get update
RUN apt-get --yes --force-yes install \
    curl \
    unzip
RUN apt-get --yes --force-yes autoremove
RUN rm -rf /var/cache/apt/archives/*

RUN curl \
    --header "JOB-TOKEN: $JOB_TOKEN" \
    "https://gitlab.com/api/v4/projects/lemra%2FAwesomeDev/jobs/$BACKEND_API_BUILD_JOB_NAME/artifacts/backend/api/build/libs/api-0.0.1-SNAPSHOT.jar" \
    --output /app.jar

#COPY --from=builder /code/awesome_dev/backend/build/libs/api-0.0.1-SNAPSHOT.jar /app.jar

CMD ["java", "-jar", "/app.jar"]
