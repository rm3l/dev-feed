#The MIT License (MIT)
#
#Copyright (c) 2019 Armel Soro
#
#Permission is hereby granted, free of charge, to any person obtaining a copy
#of this software and associated documentation files (the "Software"), to deal
#in the Software without restriction, including without limitation the rights
#to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
#copies of the Software, and to permit persons to whom the Software is
#furnished to do so, subject to the following conditions:
#
#The above copyright notice and this permission notice shall be included in all
#copies or substantial portions of the Software.
#
#THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
#IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
#FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
#AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
#LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
#OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
#SOFTWARE.

# Build
FROM adoptopenjdk:14-jdk-openj9 AS builder

WORKDIR /dev-feed-backend

COPY gradle /dev-feed-backend/gradle
COPY src /dev-feed-backend/src
COPY build.gradle /dev-feed-backend/build.gradle
COPY gradlew /dev-feed-backend/gradlew
COPY gradlew.bat /dev-feed-backend/gradlew.bat

ENV GRADLE_OPTS="-Dorg.gradle.daemon=false"

RUN ./gradlew build --stacktrace

FROM adoptopenjdk:14-jdk-openj9

COPY --from=builder /dev-feed-backend/build/libs/dev-feed-backend-0.8.0.jar /app.jar

# /graphql and /graphiql endpoints
EXPOSE 8080

# /management Actuator endpoints
EXPOSE 8081

ENTRYPOINT ["java", "-XX:+UseG1GC", "-jar", "/app.jar"]
