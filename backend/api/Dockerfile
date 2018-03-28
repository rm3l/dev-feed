FROM java:8

RUN apt-get update
RUN apt-get --yes --force-yes install \
    unzip
RUN apt-get --yes --force-yes autoremove
RUN rm -rf /var/cache/apt/archives/*

ADD ./build /build
# TODO Unzip artifact and run it

CMD ["java", "-jar", "/app.jar"]
