FROM openjdk:8-jdk-alpine AS BUILD_IMAGE
ENV APP_HOME=/root/dev/reverse-proxy/
RUN mkdir -p $APP_HOME
WORKDIR $APP_HOME
COPY . .
RUN ./gradlew build

FROM openjdk:8-jre-alpine
ENV JAVA_OPTS=""
WORKDIR /root/
COPY --from=BUILD_IMAGE /root/dev/reverse-proxy/build/libs/reverse-proxy-0.0.1-SNAPSHOT.jar ./reverse-proxy.jar
EXPOSE 8080
EXPOSE 8081
RUN apk add --no-cache curl
HEALTHCHECK --interval=5m --timeout=3s \
  CMD curl -f http://localhost:8080/health || exit 1
ENTRYPOINT exec java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /root/reverse-proxy.jar
