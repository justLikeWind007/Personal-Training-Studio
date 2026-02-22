# Local MySQL Environment

## 1. Start database

```bash
docker compose up -d mysql
```

## 2. Run app with MySQL profile

```bash
SPRING_PROFILES_ACTIVE=mysql mvn -pl ptstudio-start spring-boot:run
```

Default database settings can be overridden with environment variables:

- `DB_HOST` (default `127.0.0.1`)
- `DB_PORT` (default `3306`)
- `DB_NAME` (default `ptstudio`)
- `DB_USER` (default `ptstudio`)
- `DB_PASSWORD` (default `ptstudio`)

## 3. Flyway migration

When `mysql` profile is active, Flyway runs `classpath:db/migration/V*.sql` at startup.

## 4. Test profile

`mvn test` uses `test` profile by default and keeps Flyway disabled to ensure fast and stable CI.
