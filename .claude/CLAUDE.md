# Project: project-name

## Build and Tests
- Build: `./mvnw clean install`
- Tests: `./mvnw test`
- Run locally: `./mvnw spring-boot:run` (profile `dev`)

## Architecture
- Layers: controller -> service -> repository
- Packages organized by feature, not by layer: `com.app.prod.building`
- DTOs always kept separate from JPA entities