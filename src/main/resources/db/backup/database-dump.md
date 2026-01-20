# pg_dump - tutorial

## prerequisites
- pg_dump executable installed

## Source database
```bash
docker exec -t NAZWA_KONTENERA pg_dump -U NAZWA_UZYTKOWNIKA NAZWA_BAZY > backup.sql
```
---

## Source database
```bash
cat backup.sql | docker exec -i NAZWA_NOWEGO_KONTENERA psql -U NAZWA_UZYTKOWNIKA -d NAZWA_BAZY
```