# Spec: In-game shop info display (ghost item + text hologram)

Implementation spec for a new ShopDB plugin feature. Written to be followed step by
step; all design decisions are already made — do not revisit them, and ask before
deviating from anything marked **HARD CONSTRAINT**.

## What is being built

When a player looks at a ChestShop sign or its connected chest from within ~5
blocks while standing on the front side of the sign, a small "ghost" of the
actual traded item floats immediately above the sign itself, slowly rotating,
with floating text above it. Anchoring the complete stack to the sign keeps it
in front of any chest shop stacked directly above and never obscures the sign's
text:

```
         §6Golden Rod§r                   ← custom display name
         (item:Fishing Rod)              ← real item name (gray)
         Lure III                          ← enchant lines (gray)
         A lucky rod                       ← lore lines (light purple, italic)
         In stock (128)                    ← status word colored green/red
        [rotating item model]
```

The display is **visible only to the player looking at it** (other players see
nothing), and disappears when they look away. Every valid shop shows it — including
shops with plain items, because the floating text shows the item's *full* name,
fixing the 15-character truncated sign-name problem in game.

## Context you need

- Repo: `/Users/jesse/Development/TheatriaShopDB`. This is a Paper plugin
  (paper-api `1.21-R0.1-SNAPSHOT`, Java 21 target, builds fine on newer JDKs) that
  embeds a website + API. The feature lives entirely in the plugin (server side);
  **do not touch anything under `frontend/`**.
- Branch: create `feature/shop-info-display` **from `feature/item-details`**
  (that branch is not merged to master yet).
- **HARD CONSTRAINT — git remotes**: only ever push to `origin` (JL-III). Never
  push, PR, or merge anything to the `bricefrisco` upstream remote — that triggers
  a production AWS deploy.
- **HARD CONSTRAINT — production safety**: this runs on a live server. All world
  and entity access on the main thread only. No per-tick heavy work: the only
  per-tick cost allowed is one short ray trace per online player. Spawn/parse work
  happens only when a player's look target *changes*. Spawned entities must be
  non-persistent and cleaned up on plugin disable.
- ChestShop and this plugin's existing helpers do everything hard already. Reuse
  these; do not reimplement:
  - `ShopEventsListener.determineItemTradedByShop(Sign)` (public static) —
    resolves the sign's item line (including `#hash` codes) to an `ItemStack` via
    ChestShop's `ItemParseEvent`. Returns `null` if unresolvable.
  - `ChestShopUtil.findConnectedShopSigns(InventoryHolder)` — shop signs attached
    to a container, including either half of a double chest.
  - `ChestShopUtil.chestIsFull(ItemStack, Inventory)`.
  - `com.Acrobot.ChestShop.Signs.ChestShopSign` — `isValid(Sign)`,
    `isAdminShop(Sign)`, `isShopBlock(Block)`, line index constants
    `NAME_LINE/QUANTITY_LINE/PRICE_LINE/ITEM_LINE`.
  - `com.Acrobot.ChestShop.Utils.uBlock.findConnectedContainer(Sign)` → `Container`
    or null (null for admin shops).
  - `com.Acrobot.Breeze.Utils.BlockUtil.isSign(Block)`,
    `com.Acrobot.Breeze.Utils.InventoryUtil.getAmount(ItemStack, Inventory)`,
    `com.Acrobot.Breeze.Utils.PriceUtil.getExactBuyPrice(String)` /
    `getExactSellPrice(String)` (return `BigDecimal`; `-1` means "no such price
    on this sign").
  - `QuantityUtil.parseQuantity(String)` — the sign's per-trade quantity.
  - `ItemDetailsExtractor` — copy its **item-flag rules** (which enchants to show,
    see §Text content below), but do NOT reuse its legacy-string serialization:
    in-game we use Adventure `Component`s directly.

## New files

All in a new package `com.playtheatria.shopdb.display`:

| File | Responsibility |
|---|---|
| `ShopInfoDisplayService.java` | Repeating scan task, per-player state map, spawn/despawn/refresh, cleanup. Implements `Listener` for quit/world-change. |
| `ActiveDisplay.java` | Plain mutable holder for one player's current display (package-private is fine). |
| `ShopInfoTextBuilder.java` | Static `Component` builder: item identity lines + stock line, plus a pure rendered-line estimate for dynamic scaling. No Bukkit scheduler/world access, so it stays testable. |

Modified files: `ShopDBPlugin.java` (wiring), `src/main/resources/config.yml`
(new section), `pom.xml` + `src/main/resources/plugin.yml` (version → `4.2.0`).
No new commands, no new permissions, no database changes.

## Behavior specification

### 1. The scan task

`ShopInfoDisplayService.start()` registers itself as a `Listener` and starts a
`BukkitRunnable` repeating every `scan-interval-ticks` (config, default 4). Each
run, for every online player:

1. `Block target = player.getTargetBlockExact(rangeBlocks);` (config
   `range-blocks`, default 5). This hits signs — vanilla ray tracing does not
   ignore passable blocks.
2. Resolve the target to a valid sign:
   - If `BlockUtil.isSign(target)`, use its `Sign` state.
   - Else if `ChestShopSign.isShopBlock(target)`, iterate
     `ChestShopUtil.findConnectedShopSigns(container.getInventory().getHolder())`
     and use the first eligible sign. The inventory-holder overload ensures the
     unsigned half of a double chest still resolves the sign on the other half.
   - A sign is eligible only when `ChestShopSign.isValid(sign)` and the player's
     location is on its front side. Read the outward face from `Directional` or
     `Rotatable` block data and require a positive horizontal dot product from
     the sign center to the player. This keeps chest targeting unambiguous from
     behind a shop.
   - Otherwise, no shop is targeted.
3. Compare with the player's `ActiveDisplay` (a `HashMap<UUID, ActiveDisplay>`,
   main-thread only):
   - **No shop targeted** → tolerate one missed scan while the player remains
     in front of the same still-valid sign, continuing the current rotation. On
     the second consecutive miss, despawn it and remove the map entry. This
     prevents the thin sign hitbox from causing destroy/respawn blinking.
   - **Same shop still targeted** (same sign `Location` AND the sign's
     `getLine(ITEM_LINE)` is unchanged from what was stored) → keep it. Advance
     its rotation (see §3), reset the missed-scan counter, and, every
     `stock-refresh-ticks` (config, default 20), rebuild the text component and
     reapply its dynamic scale so the stock status stays current (no respawn).
   - **Different shop / sign line changed** → switch to the new shop on its
     first valid observation: despawn the old pair and spawn the replacement.
     Initial activation is also immediate. The missed-target grace still absorbs
     empty-space and thin-sign-hitbox scans without making deliberate transitions
     between valid shops lag behind the player's crosshair.

Spawning a new display:

- `ItemStack item = ShopEventsListener.determineItemTradedByShop(sign);`
  If `item == null` (unparseable item line), store nothing and show nothing.
- Clone it and set amount 1 for the ghost: `ItemStack shown = item.clone(); shown.setAmount(1);`
- Build the text component (§4), spawn the two entities (§2), store an
  `ActiveDisplay { Location signLocation; String itemLine; ItemStack item;
  ItemDisplay itemEntity; TextDisplay textEntity; float spinAngleDeg; int
  ticksSinceStockRefresh; int missedTargetScans; }`.

This design means `ItemParseEvent` fires only when a player *starts* looking at a
shop — never per tick.

Before reusing an active pair, require both display entities to remain valid;
otherwise remove any survivor and recreate the pair. Isolate runtime exceptions
per player so one broken display cannot starve the remaining online-player scan;
log only the first consecutive failure for that player.

### 2. The entities (per-player visibility)

Both entities are spawned with the pre-spawn consumer overload so they are never
visible to anyone before configuration is applied:

```java
Location base = sign.getLocation().toCenterLocation();
World world = base.getWorld();

TextDisplay text = world.spawn(base.clone().add(0, TEXT_Y_OFFSET, 0), TextDisplay.class, e -> {
    e.setVisibleByDefault(false);          // nobody sees it...
    e.setPersistent(false);                // never saved to disk
    e.addScoreboardTag(ENTITY_TAG);        // "shopdb_info_display"
    updateText(e, component);              // text + line-count-based scale
    e.setBillboard(Display.Billboard.CENTER);
    e.setAlignment(TextDisplay.TextAlignment.CENTER);
    e.setShadowed(false);
    e.setSeeThrough(false);
    e.setBackgroundColor(Color.fromARGB(0xB0, 0, 0, 0)); // dim translucent black
});
player.showEntity(plugin, text);           // ...except this player

ItemDisplay ghost = world.spawn(base.clone().add(0, ITEM_Y_OFFSET, 0), ItemDisplay.class, e -> {
    e.setVisibleByDefault(false);
    e.setPersistent(false);
    e.addScoreboardTag(ENTITY_TAG);
    e.setItemStack(shown);
    e.setBillboard(Display.Billboard.FIXED); // we rotate it ourselves
    e.setTransformation(new Transformation(
            new Vector3f(), new Quaternionf(), new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE), new Quaternionf()));
});
player.showEntity(plugin, ghost);
```

Constants (plain `private static final` in the service, not config):
`TEXT_Y_OFFSET = 0.80`, `ITEM_Y_OFFSET = 0.65`,
`MAX_TEXT_SCALE = 0.4f`, `MIN_TEXT_SCALE = 0.16f`,
`TEXT_LINES_AT_MAX_SCALE = 6`, `ITEM_SCALE = 0.25f`,
`TARGET_MISS_GRACE_SCANS = 2`,
`TARGET_CHANGE_CONFIRMATION_SCANS = 1`,
`ENTITY_TAG = "shopdb_info_display"`, `SPIN_SECONDS_PER_ROTATION = 6`.
`Vector3f`/`Quaternionf` are `org.joml` — provided transitively by paper-api.

Text is bottom-anchored and the item is positioned below that anchor. Both
entities use the sign block center as their base, so they float above the sign
and in front of any chest stacked in the block above it; text grows upward from
its anchor as lines are added. Estimate the rendered line count from explicit
newlines plus one line per 32 characters to account for the client's default
200-pixel wrapping. Up to 6 estimated lines use scale `0.4`; longer text scales
proportionally (`0.4 * 6 / lineCount`) down to a minimum of `0.16`. Apply the
scale inside the pre-spawn consumer and again whenever stock refresh rebuilds the
component. Do not change `lineWidth`; client wrapping preserves all content
instead of truncating it. The tighter offsets leave only a small clearance from
the sign to the item and from the item to the text anchor.

Two players looking at the same shop each get their own pair of entities; each
sees only their own. Display entities have no hitbox, so they cannot intercept the
right-click that buys from the sign.

### 3. Rotation

Each scan tick, for every ActiveDisplay:

```java
active.spinAngleDeg += 360f * (scanIntervalTicks / 20f) / SPIN_SECONDS_PER_ROTATION;
active.itemEntity.setInterpolationDelay(0);
active.itemEntity.setInterpolationDuration(scanIntervalTicks);
active.itemEntity.setTransformation(new Transformation(
        new Vector3f(),
        new Quaternionf().rotationY((float) Math.toRadians(active.spinAngleDeg)),
        new Vector3f(ITEM_SCALE, ITEM_SCALE, ITEM_SCALE),
        new Quaternionf()));
```

The client interpolates between steps, so the spin looks smooth.

### 4. Text content (`ShopInfoTextBuilder`)

One `Component` with lines joined by `Component.newline()`. Order:

**Item identity.** If `meta.hasDisplayName()`: put the custom name component
(`meta.displayName()`, used as-is, its colors intact) on the first line, then put
the real item name on the next line as gray `(item:<item_name>)` text, for example
`(item:Fishing Rod)`. If no custom name: use just one line containing the real
item name in white. The real item name is the client-localized translatable:

```java
Component realName = Component.translatable(item.getType()); // Paper: Material implements Translatable
```

Fallback if that overload will not compile: prettify the material name in Java —
`item.getType().name().toLowerCase(Locale.ROOT)`, split on `_`, capitalize each
word, join with spaces — as a literal `Component.text(...)`.

**Enchant lines** — one per enchant, using the exact same visibility rules as
`ItemDetailsExtractor.details(...)` (regular enchants unless `HIDE_ENCHANTS`;
stored enchants of `EnchantmentStorageMeta` unless `HIDE_STORED_ENCHANTS` or
`HIDE_ADDITIONAL_TOOLTIP`). Render each as
`enchantment.displayName(level).colorIfAbsent(NamedTextColor.GRAY)` — Paper's
`Enchantment#displayName(int)` returns the vanilla-styled name with roman level.

**Lore lines** — `meta.lore()` components as-is, but defaulted to tooltip style
when unstyled:

```java
line.colorIfAbsent(NamedTextColor.LIGHT_PURPLE)
    .decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.TRUE)
```

Show every lore line, including blank spacer lines. Do not cap or replace lore
with an ellipsis; dynamic text scaling keeps long tooltips compact.

**Stock line** (last). Only the status word/phrase is colored — any count suffix
is gray. Rules, in order:

| Case | Line |
|---|---|
| Admin shop (`ChestShopSign.isAdminShop(sign)`) | `Always in stock` — green |
| Container missing (`uBlock.findConnectedContainer(sign) == null`, non-admin) | no stock line |
| Sign has a buy price (`PriceUtil.getExactBuyPrice(priceLine)` ≥ 0), stock ≥ quantity | `In stock` green + ` (N)` gray, N = `InventoryUtil.getAmount(item, container.getInventory())` |
| Sign has a buy price, stock < quantity | `Out of stock` — red |
| Additionally, if the sign has a sell price and `ChestShopUtil.chestIsFull(item, inventory)` | extra line `Shop full` — red (players can't sell to it) |

(quantity = `QuantityUtil.parseQuantity(sign.getLine(QUANTITY_LINE))`.)

Note for the builder's signature: pass in everything it needs
(`ItemStack item, boolean adminShop, Integer stockCount /*null = no line*/,
Integer quantity, boolean hasBuyPrice, boolean showShopFull`) so the class itself
never touches Bukkit world state — the service computes those from the sign and
container. That keeps the builder unit-testable.

### 5. Cleanup — every path

- **Look away / target change / sign no longer valid**: handled by the scan loop
  (despawn = `entity.remove()` on both, remove map entry).
- **`PlayerQuitEvent` and `PlayerChangedWorldEvent`**: listener methods despawn
  that player's ActiveDisplay. (Teleports within a world need nothing — the next
  scan tick sees the target changed.)
- **`stop()`** (called from `ShopDBPlugin.stopServices()`): cancel the task,
  despawn all ActiveDisplays, `HandlerList.unregisterAll(this)`.
- **Startup sweep** (in `start()`): for each loaded world,
  `world.getEntitiesByClass(Display.class)` and `remove()` any whose
  `getScoreboardTags().contains(ENTITY_TAG)` — clears leftovers from a crashed
  reload. Entities are non-persistent so a full restart never leaves any.

## Wiring in `ShopDBPlugin`

Add a field `private ShopInfoDisplayService shopInfoDisplay;`. In
`startServices()`, after the HTTP server starts (independent of
`updater.enabled` — this feature needs ChestShop but not WorldGuard):

```java
if (getConfig().getBoolean("shop-info-display.enabled", true)) {
    if (getServer().getPluginManager().getPlugin("ChestShop") != null) {
        shopInfoDisplay = new ShopInfoDisplayService(this,
                getConfig().getInt("shop-info-display.scan-interval-ticks", 4),
                getConfig().getInt("shop-info-display.range-blocks", 5),
                getConfig().getInt("shop-info-display.stock-refresh-ticks", 20));
        shopInfoDisplay.start();
        getLogger().info("Shop info display started.");
    } else {
        getLogger().warning("shop-info-display.enabled is true but ChestShop is not installed - skipping.");
    }
}
```

In `stopServices()`, first thing: `if (shopInfoDisplay != null) { shopInfoDisplay.stop(); shopInfoDisplay = null; }`.
`/shopdb reload` already routes through stop/start, so it needs nothing new.

Append to `config.yml` (the existing `refreshConfigFile()` merges new keys into
live configs automatically on upgrade):

```yaml
# Floating item + text shown to a player looking at a chest shop sign (or its
# chest) from up to range-blocks away. Per-player: nobody else sees it.
shop-info-display:
  enabled: true
  # How often (in ticks) player look-targets are checked. Lower = snappier.
  scan-interval-ticks: 4
  # Max distance (blocks) at which the display appears.
  range-blocks: 5
  # How often (in ticks) the stock line refreshes while a player keeps looking.
  stock-refresh-ticks: 20
```

Version bump: `pom.xml` `<version>` and `plugin.yml` `version` → `4.2.0`.

## What NOT to do

- No `ItemParseEvent` per tick — only on target change (see §1).
- No entities visible by default; `setVisibleByDefault(false)` must be set inside
  the spawn consumer, not after spawn.
- No persistence: never skip `setPersistent(false)`.
- No async access to worlds, blocks, or entities.
- No new dependencies in `pom.xml`, no database or web/API changes, no frontend
  changes, no changes to the updater/rescanner.
- Do not "fix" or refactor unrelated code you pass by.

## Build, test, deliver

```
cd /Users/jesse/Development/TheatriaShopDB
git checkout feature/item-details && git pull origin feature/item-details
git checkout -b feature/shop-info-display
# ...implement...
mvn -q test        # all existing tests must stay green
make build         # stages frontend + mvn package -> target/ShopDB-4.2.0.jar
```

Add unit tests for `ShopInfoTextBuilder`'s stock-line logic and rendered-line
estimate, plus the dynamic scale bounds, front-side calculation, missed-scan
grace boundary, and target-change confirmation boundary; these can run without
a Bukkit server. Entity behavior is validated manually. Commit on
`feature/shop-info-display` with a clear message and push to `origin` only.

## Manual acceptance checklist (test server)

1. From the sign's front side, look at a normal shop sign or its connected chest
   from ≤5 blocks: the enlarged compact text with the rotating item below it
   floats just above the sign, leaving every sign line unobstructed. With chest
   shops stacked vertically, the lower shop's display remains in front of the
   upper chest instead of inside it. Sign/chest transitions do not blink or reset
   the spin, and a different valid shop replaces the display on the next scan.
   Look away: gone after two missed scans (~8 ticks); target the chest from behind
   the sign: no display.
2. A second account looking at the same shop sees its own display; the first
   account sees exactly one.
3. Shop with a truncated sign item name (e.g. an armor trim template): full item
   name shows.
4. Enchanted-book shop: stored enchants listed gray; item with `HIDE_ENCHANTS`
   flag: none listed. A long item such as the Titan Axe scales down enough that
   every line remains visible.
5. Custom-named item: colored name on the first line, gray
   `(item:<item_name>)` on the next line; lore in light purple italic. Confirm
   every lore line shown in the vanilla tooltip is present in the hologram,
   including lines beyond the first 10.
6. Stock: empty the chest while looking — line flips to red `Out of stock` within
   ~1 second. Refill — flips back green.
7. Sell sign with a full chest: red `Shop full` line.
8. Admin shop: green `Always in stock`, no errors despite no container.
9. Break the sign while a player is looking: display disappears, no console
   errors.
10. `/shopdb reload`: displays vanish and the feature comes back; no orphaned
    display entities afterward.
11. TPS unaffected with several players looking around (`spark tps` or similar).
