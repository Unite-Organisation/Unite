#!/bin/bash

# Run Maven with flyway migration
./mvnw flyway:migrate \
  -Dflyway.locations="classpath:db/migration,classpath:db/test-data"