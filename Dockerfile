ARG JDK_VERSION=17-jdk-slim

FROM openjdk:$JDK_VERSION

COPY . /tmp
WORKDIR /tmp

RUN --mount=type=secret,id=NEXUS_URL \
    --mount=type=secret,id=NEXUS_LOGIN \
    --mount=type=secret,id=NEXUS_PASSWORD \
    ./gradlew -b build.gradle clean publish -PnexusUrl=$(cat /run/secrets/NEXUS_URL) -PnexusLogin=$(cat /run/secrets/NEXUS_LOGIN) -PnexusPassword=$(cat /run/secrets/NEXUS_PASSWORD)
