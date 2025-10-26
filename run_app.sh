#!/bin/bash
set -euo pipefail

echo "Building .jar file"
if ! ./mvnw clean install -DskipJooq=true -DskipTests; then
  echo "❌ Maven build failed. Check the logs above."
  exit 1
fi

echo "Running docker containers"
if ! docker compose up --build; then
  echo "❌ Docker containers failed to start. Check docker-compose logs above."
  exit 1
fi