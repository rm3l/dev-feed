FROM java:8

ARG JOB_TOKEN
ARG BACKEND_API_BUILD_JOB_NAME

RUN apt-get update
RUN apt-get --yes --force-yes install \
    curl \
    unzip
RUN apt-get --yes --force-yes autoremove
RUN rm -rf /var/cache/apt/archives/*

ARG DL_URL="https://gitlab.com/api/v4/projects/lemra%2FAwesomeDev/jobs/$BACKEND_API_BUILD_JOB_NAME/artifacts/backend/api/build/libs/api-0.0.1-SNAPSHOT.jar"

# Just for debugging
RUN echo "JOB_TOKEN: [${JOB_TOKEN}]"
RUN echo "BACKEND_API_BUILD_JOB_NAME: [${BACKEND_API_BUILD_JOB_NAME}]"
RUN curl \
    -sSL \
    --header "JOB-TOKEN: $JOB_TOKEN" \
    $DL_URL \
    --output /dev/null


RUN curl \
    -sSL \
    --header "JOB-TOKEN: $JOB_TOKEN" \
    $DL_URL \
    --output /app.jar

CMD ["java", "-jar", "/app.jar"]
