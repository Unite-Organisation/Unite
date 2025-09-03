#!/bin/bash

set -e

echo "Building .jar file"
./mvnw clean install -DskipJooq=true -DskipTests

echo "Running docker containers"
docker-compose up --build

