ARG JDK_VERSION=17-jdk-slim

FROM openjdk:$JDK_VERSION

COPY . /tmp
WORKDIR /tmp

RUN --mount=type=secret,id=NEXUS_URL \
    --mount=type=secret,id=NEXUS_PASSWORD \
    ./gradlew -b build.gradle clean publish -PbackstageNexusUrl=$(cat /run/secrets/NEXUS_URL) -PbackstageNexusToken=$(cat /run/secrets/NEXUS_PASSWORD) -PbackstageNexusDeployToken=$(cat /run/secrets/NEXUS_PASSWORD)
