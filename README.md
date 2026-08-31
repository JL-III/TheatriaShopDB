# ShopDB

Paper plugin that runs the entire ShopDB stack from inside the game server —
the same pattern as MC-Ledger. One jar in `plugins/` does everything:

- serves the shop browser website at `/` and the REST API under `/api/v3`,
  backed by a SQLite file in the plugin's data folder;
- listens to ChestShop events (create/destroy/transaction/restock), buffers
  them in `shop_events.db`, and posts them to its own API on the configured
  interval and on shutdown — the ShopDB-Updater plugin's full pipeline,
  absorbed (requires the ChestShop and WorldGuard plugins; without them the
  updater half disables itself and the website/API still run); when Lands is
  installed, chest shops on player-owned lands can also be published;
- lets the exact Lands owner use `/shopdb list` or `/shopdb unlist` while
  standing inside their land;
- provides admin-only `/shopdb list|unlist <market-stall>`, `/shopdb reload`,
  `/shopdb rescan`, and `/shopdbedit delete x y z` workflows (permission
  `theatria.shopdb.admin`).

No cloud services, no separate updater plugin, no shell access needed.

## Build

```
make build
```

Produces `target/ShopDB-<version>.jar`, ready to drop into `plugins/`.
Requires JDK 21+ (any modern JDK works) and Node via asdf (pinned in
`.tool-versions`; react-scripts 4 needs a Node of that era).

The website calls the API on whatever origin it is served from (see
`frontend/src/backend.js`), so the same jar works on an IP, a test domain, or
the production domain with no rebuilds. `REACT_APP_BACKEND=<url> make build`
overrides that with a fixed URL if ever needed.

`mvn package` alone builds an API-only jar without the website.

## Configure

`plugins/ShopDB/config.yml` (created on first start):

- `port` — HTTP port for the website + API (default 8080).
- `api-username` / `api-key` — credentials for `POST /chest-shops` and
  `PUT`/`DELETE /regions` (what the ShopDB-Updater plugin sends). Set
  `api-key` to the updater's key and the plugin provisions the bcrypt row in
  the `users` table itself on startup — no database access needed. Leave
  `api-key` empty to manage the `users` table externally (e.g. when the row
  comes from an imported production dump).
- `database-file` — SQLite file name inside the plugin data folder.

Deployment never needs a shell on the host: upload the jar, edit
`plugins/ShopDB/config.yml`, restart. To import existing data, build the
SQLite file elsewhere and upload it as `plugins/ShopDB/shopdb.db` — the schema
matches the previous backend's PostgreSQL schema 1:1, so the production dump
converts table-for-table (booleans as 0/1, timestamps as epoch millis).

Rich item search uses the resolved `base_material` and item metadata captured
by the updater. After upgrading an existing database, run `/shopdb rescan`
once so legacy shops can be filtered by book type, enchantment, and level.
Display-visible enchantments are searchable normally. Hidden enchantments are
searchable only when trusted Titan lore (`Ancient Power Ω`) independently
confirms the enchantment and level, keeping cosmetic sheen enchants out of
search results.

Current WorldGuard locations are migrated to `MARKET_STALL`. Lands locations
are stored separately as `PLAYER_SHOP` using the Land ULID, so renames and
same-named market/player locations cannot collide. Player publishing is
owner-only by UUID; trusted Lands members and ShopDB admin permission do not
substitute for ownership.

## Develop

Run the web server without a Paper server:

```
java -cp "target/ShopDB-<version>.jar:<path-to-gson.jar>" com.playtheatria.shopdb.DevMain 8080 shopdb.db updater
```

(gson is provided by Paper at runtime, so it must be added to the classpath
for standalone runs.)

`mvn test` runs the unit tests. `make help` lists build targets.
