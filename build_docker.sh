#!/bin/bash
JAVA_HOME=/usr/lib/jvm/java-11-openjdk-11.0.7.10-0.fc30.x86_64/ ./mvnw clean package \
         -D quarkus.package.type=fast-jar \
         -Dquarkus.container-image.build=true \
         -Dquarkus.container-image.group=mikeyaustin \
         -Dquarkus.container-image.push=true
