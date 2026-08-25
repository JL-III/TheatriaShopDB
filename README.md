# TheatriaShopDB

Backend API for ShopDB. Ingests chest shop sign events from the ShopDB-Updater
plugin and serves the shop browser API under `/api/v3`.

Runs as a single self-hosted process on the Minecraft server host — plain HTTP
backed by a local SQLite file, following the same pattern as MC-Ledger. No
cloud services involved.

## Build

```
mvn package
```

Produces `target/shopdb-<version>-runner.jar` (single runnable uber-jar).

## Run

```
QUARKUS_DATASOURCE_JDBC_URL=jdbc:sqlite:/path/to/shopdb.db \
QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=update \
QUARKUS_HTTP_PORT=8080 \
SHOPDB_API_USERNAME=<api user name> \
java -jar target/shopdb-*-runner.jar
```

- `QUARKUS_DATASOURCE_JDBC_URL` — path to the SQLite database file (created on
  first run when `QUARKUS_HIBERNATE_ORM_DATABASE_GENERATION=update`).
- `SHOPDB_API_USERNAME` — username of the row in the `users` table whose bcrypt
  password hash authenticates `POST /chest-shops` (the updater's API key).
- The server listens on `0.0.0.0`; point the ShopDB-Updater plugin's `API URI`
  config at `http://127.0.0.1:<port>/api/v3/` when running on the same host.
