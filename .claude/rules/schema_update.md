## Schema update documentation

1. Make sure that postgres container is running, if not use command below
```shell
docker compose up -d db
```

2. Write Flyway schema migration - use propper numeration
3. Run script to apply Flyway migrations
```shell
./flyway_migrate.sh
```
4. Run script to regenerate Jooq
```shell
./jooq_generate.sh
```