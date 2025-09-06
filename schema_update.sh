#!/bin/bash

docker compose down -v db
docker compose up -d db
./mvnw flyway:migrate
./mvnw clean jooq-codegen:generate