#!/bin/bash
set -e

mvn install -DskipTests -Dmaven.javadoc.skip=true

echo "$DOCKER_PASSWORD" | docker login -u "$DOCKER_USERNAME" --password-stdin

echo "Start building images"
echo "Using Tag: "
echo $TAG

cd FROST-Server.HTTP
export artifact=FROST-Server.HTTP-${version}.war
echo "artifact: "
echo $artifact
docker buildx build --build-arg WAR_FILE=$artifact --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server-http:$TAG --push .
cd ..

cd FROST-Server.MQTT
export artifact=FROST-Server.MQTT-${version}-jar-with-dependencies.jar
echo "artifact: "
echo $artifact
docker buildx build --build-arg JAR_FILE=$artifact --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server-mqtt:$TAG --push .
cd ..

cd FROST-Server.MQTTP
export artifact=FROST-Server.MQTTP-${version}.war
echo "artifact: "
echo $artifact
docker buildx build --build-arg WAR_FILE=$artifact --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server:$TAG --push .
cd ..

if [ "${TRAVIS_BRANCH}" == "master" ]; then

  docker tag fraunhoferiosb/frost-server-http:$TAG fraunhoferiosb/frost-server-http:latest
  docker tag fraunhoferiosb/frost-server-mqtt:$TAG fraunhoferiosb/frost-server-mqtt:latest
  docker tag fraunhoferiosb/frost-server:$TAG fraunhoferiosb/frost-server:latest

  docker push fraunhoferiosb/frost-server-http:latest
  docker push fraunhoferiosb/frost-server-mqtt:latest
  docker push fraunhoferiosb/frost-server:latest
fi
