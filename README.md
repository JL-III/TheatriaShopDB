# ShopDB

Paper plugin that runs the entire ShopDB stack from inside the game server —
the same pattern as MC-Ledger. One jar in `plugins/` serves the shop browser
website at `/` and the REST API under `/api/v3`, backed by a SQLite file in the
plugin's data folder. No cloud services.

The ShopDB-Updater plugin keeps posting chest shop events exactly as before —
point its `API URI` config at `http://127.0.0.1:<port>/api/v3/`.

## Build

```
make build
```

Produces `target/ShopDB-<version>.jar`, ready to drop into `plugins/`.
Requires JDK 21+ (any modern JDK works) and Node via asdf (pinned in
`.tool-versions`; react-scripts 4 needs a Node of that era).

The absolute API URL baked into the website comes from
`frontend/.env.production` — update it when the deployment host changes, or
override per-build with `REACT_APP_BACKEND=<url> make build`.

`mvn package` alone builds an API-only jar without the website.

## Configure

`plugins/ShopDB/config.yml` (created on first start):

- `port` — HTTP port for the website + API (default 8080).
- `api-username` — the `users` table row whose bcrypt password hash
  authenticates `POST /chest-shops` and `PUT`/`DELETE /regions` (the
  updater's API key).
- `database-file` — SQLite file name inside the plugin data folder.

Seed the API user once (hash is a standard `$2a$` bcrypt of the API key):

```
sqlite3 plugins/ShopDB/shopdb.db "INSERT INTO users (username, password) VALUES ('updater', '<bcrypt hash>');"
```

The schema matches the previous backend's PostgreSQL schema 1:1, so the
production dump imports table-for-table (booleans as 0/1, timestamps as epoch
millis).

## Develop

Run the web server without a Paper server:

```
java -cp "target/ShopDB-<version>.jar:<path-to-gson.jar>" com.playtheatria.shopdb.DevMain 8080 shopdb.db updater
```

(gson is provided by Paper at runtime, so it must be added to the classpath
for standalone runs.)

`mvn test` runs the unit tests. `make help` lists build targets.
