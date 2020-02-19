#!/bin/bash
set -e

mvn install -DskipTests -Dmaven.javadoc.skip=true

printf "Start building images"
ls
cd FROST-Server.HTTP && docker buildx build --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server-http:latest . && cd ..
cd FROST-Server.MQTT && docker buildx build --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server-mqtt:latest . && cd ..
cd FROST-Server.MQTTP && docker buildx build --platform amd64,linux/arm64/v8,linux/arm/v7 --tag fraunhoferiosb/frost-server:latest . && cd ..

if [ "${TRAVIS_BRANCH}" = "master" ]; then
  docker push fraunhoferiosb/frost-server-http:latest
  docker push fraunhoferiosb/frost-server-mqtt:latest
  docker push fraunhoferiosb/frost-server:latest
fi
echo "TAG:"
echo $TAG
#docker tag fraunhoferiosb/frost-server-http:latest fraunhoferiosb/frost-server-http:$TAG
#docker tag fraunhoferiosb/frost-server-mqtt:latest fraunhoferiosb/frost-server-mqtt:$TAG
#docker tag fraunhoferiosb/frost-server:latest fraunhoferiosb/frost-server:$TAG

#docker push fraunhoferiosb/frost-server-http:$TAG
#docker push fraunhoferiosb/frost-server-mqtt:$TAG
#docker push fraunhoferiosb/frost-server:$TAG
