#!/usr/bin/env sh
set -u

echo "Before Docker compose build"
docker-compose build
echo "Docker compose build"
export PROXY_NODE_URL="http://$(minikube ip)"
docker-compose up --abort-on-container-exit | grep acceptance-tests_1 --colour=never
docker cp $(docker ps -a -q -f name="acceptance-tests"):/testreport .
docker-compose down
