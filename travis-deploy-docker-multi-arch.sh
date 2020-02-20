#!/bin/bash
set -e

mvn install -DskipTests -Dmaven.javadoc.skip=true

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

echo "Start building images"
echo "Using Tag: "
echo $TAG
cd FROST-Server.HTTP && docker buildx build --build-arg WAR_FILE=FROST-Server.HTTP-${version}.war --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server-http:$TAG --push . && cd ..
cd FROST-Server.MQTT && docker buildx build --build-arg JAR_FILE=FROST-Server.MQTT-${version}-jar-with-dependencies.jar --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server-mqtt:$TAG --push . && cd ..
cd FROST-Server.MQTTP && docker buildx build --build-arg WAR_FILE=FROST-Server.MQTTP-${version}.war --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server:$TAG --push . && cd ..

if [ "${TRAVIS_BRANCH}" == "master" ]; then

  docker tag fraunhoferiosb/frost-server-http:$TAG fraunhoferiosb/frost-server-http:latest
  docker tag fraunhoferiosb/frost-server-mqtt:$TAG fraunhoferiosb/frost-server-mqtt:latest
  docker tag fraunhoferiosb/frost-server:$TAG fraunhoferiosb/frost-server:latest

  docker push fraunhoferiosb/frost-server-http:latest
  docker push fraunhoferiosb/frost-server-mqtt:latest
  docker push fraunhoferiosb/frost-server:latest
fi
