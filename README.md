# TheatriaShopDB

Backend API for ShopDB. Ingests chest shop sign events from the ShopDB-Updater
plugin and serves the shop browser API under `/api/v3`.

Runs as a single self-hosted process on the Minecraft server host — plain HTTP
backed by a local SQLite file, following the same pattern as MC-Ledger and
TheatriaMarket. No cloud services involved. The React frontend (in `frontend/`)
is built into the jar and served at `/`; the API keeps its `/api/v3` prefix.

## Build

```
make build
```

Produces `target/shopdb-<version>-runner.jar` (single runnable uber-jar
containing the API and the website). The absolute API URL baked into the
frontend bundle comes from `frontend/.env.production` — update it when the
deployment host changes, or override per-build with
`REACT_APP_BACKEND=<url> make build`.

Toolchain: JDK 11–17 for Maven/Quarkus; Node is pinned via `.tool-versions`
(asdf) — react-scripts 4 needs a Node from that era. `make help` lists all
targets; `mvn package` alone builds an API-only jar without the website.

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

Note: the site uses browser-side routing (`BrowserRouter`), so a page refresh
on a deep link (e.g. `/players/<name>`) 404s when hitting the jar directly.
When a reverse proxy (Caddy/nginx) fronts the server for TLS, add a fallback
rewrite of unmatched non-`/api` paths to `/index.html`.
