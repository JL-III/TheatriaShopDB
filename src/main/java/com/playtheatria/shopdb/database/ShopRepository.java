package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.ItemType;
import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.TradeType;
import com.playtheatria.shopdb.models.ShopLocationType;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * All queries mirror the previous backend's Panache/HQL queries one-to-one,
 * including their quirks (e.g. the list-query server filter binds "The_Ark"
 * while the column stores "THE_ARK", so it matches nothing — preserved for
 * response parity). ORDER BY adds explicit null placement to match PostgreSQL
 * (ASC = nulls last, DESC = nulls first); SQLite's default is the opposite.
 */
public class ShopRepository {
    private final Db db;

    // item_details is intentionally a VARCHAR for compatibility with imported
    // databases. Always guard JSON table functions so one malformed legacy row
    // cannot break search for every shop.
    private static final String SAFE_ITEM_DETAILS =
            "CASE WHEN json_valid(cs.item_details) THEN cs.item_details ELSE '{}' END";
    private static final String SAFE_ENCHANT_OBJECT =
            "CASE WHEN enchant.type = 'object' THEN enchant.value ELSE '{}' END";
    private static final String SAFE_FACET_ENCHANT_OBJECT =
            "CASE WHEN facet_enchant.type = 'object' THEN facet_enchant.value ELSE '{}' END";
    // searchEnchants is authoritative when present, including an explicitly
    // empty array (used to keep cosmetic sheen enchantments out of search).
    // Rows written before searchEnchants existed fall back to visible enchants.
    private static final String SEARCH_ENCHANT_PATH =
            "CASE WHEN json_type(" + SAFE_ITEM_DETAILS + ", '$.searchEnchants') IS NOT NULL " +
                    "THEN '$.searchEnchants' ELSE '$.enchants' END";
    private static final String SEARCH_ENCHANTS =
            "json_each(" + SAFE_ITEM_DETAILS + ", " + SEARCH_ENCHANT_PATH + ")";
    private static final String HAS_ENCHANTMENTS =
            "EXISTS (SELECT 1 FROM " + SEARCH_ENCHANTS + " enchant " +
                    "WHERE json_type(" + SAFE_ENCHANT_OBJECT + ", '$.name') = 'text' " +
                    "AND trim(json_extract(" + SAFE_ENCHANT_OBJECT + ", '$.name')) <> '')";
    private static final String IS_BOOK =
            "lower(coalesce(cs.base_material, '')) IN " +
                    "('book', 'writable_book', 'written_book', 'enchanted_book', 'knowledge_book')";
    private static final String IS_ENCHANTED_BOOK =
            "lower(coalesce(cs.base_material, '')) = 'enchanted_book'";

    private static final String SELECT =
            "SELECT cs.id, cs.server, cs.x, cs.y, cs.z, cs.material, cs.owner_id, cs.town_id, " +
                    "cs.quantity, cs.quantity_available, cs.buy_price, cs.sell_price, " +
                    "cs.buy_price_each, cs.sell_price_each, cs.is_full, cs.is_hidden, " +
                    "cs.is_buy_sign, cs.is_sell_sign, cs.base_material, cs.item_details, " +
                    "cs.display_name_plain, " +
                    "p.name AS owner_name, r.name AS town_name, r.location_type AS town_type " +
                    "FROM chest_shop_sign cs " +
                    "LEFT JOIN player p ON p.id = cs.owner_id " +
                    "LEFT JOIN region r ON r.id = cs.town_id ";

    private static final String LIST_WHERE =
            "WHERE (?1 = '' OR cs.material = ?1) " +
                    "AND (?2 = '' OR cs.display_name_plain = ?2 COLLATE NOCASE) " +
                    "AND (?3 = '' " +
                    "OR instr(lower(cs.material), lower(?3)) > 0 " +
                    "OR (trim(replace(?3, '_', '')) <> '' " +
                    "    AND instr(replace(lower(cs.material), '_', ' '), " +
                    "              replace(lower(?3), '_', ' ')) > 0) " +
                    "OR instr(lower(coalesce(cs.base_material, '')), lower(?3)) > 0 " +
                    "OR (trim(replace(?3, '_', '')) <> '' " +
                    "    AND instr(replace(lower(coalesce(cs.base_material, '')), '_', ' '), " +
                    "         replace(lower(?3), '_', ' ')) > 0) " +
                    "OR instr(lower(coalesce(cs.display_name_plain, '')), lower(?3)) > 0 " +
                    "OR EXISTS (SELECT 1 FROM " + SEARCH_ENCHANTS + " enchant " +
                    "           WHERE enchant.type = 'object' " +
                    "           AND (instr(lower(coalesce(json_extract(" + SAFE_ENCHANT_OBJECT +
                    ", '$.name'), '')), lower(?3)) > 0 " +
                    "                OR (trim(replace(?3, '_', '')) <> '' " +
                    "                    AND instr(replace(lower(coalesce(json_extract(" + SAFE_ENCHANT_OBJECT +
                    ", '$.name'), '')), '_', ' '), replace(lower(?3), '_', ' ')) > 0) " +
                    "                OR instr(replace(lower(coalesce(json_extract(" + SAFE_ENCHANT_OBJECT +
                    ", '$.name'), '')), '_', ' ') || ' ' || " +
                    "                         CAST(coalesce(CAST(json_extract(" + SAFE_ENCHANT_OBJECT +
                    ", '$.level') AS INTEGER), 0) AS TEXT), " +
                    "                         replace(lower(?3), '_', ' ')) > 0)) " +
                    "OR EXISTS (SELECT 1 FROM json_each(" + SAFE_ITEM_DETAILS + ", '$.lore') lore " +
                    "           WHERE instr(lower(CAST(lore.value AS TEXT)), lower(?3)) > 0)) " +
                    "AND (?4 = '' OR EXISTS " +
                    "    (SELECT 1 FROM " + SEARCH_ENCHANTS + " enchant " +
                    "     WHERE enchant.type = 'object' " +
                    "     AND replace(lower(coalesce(json_extract(" + SAFE_ENCHANT_OBJECT +
                    ", '$.name'), '')), ' ', '_') = replace(lower(?4), ' ', '_') " +
                    "     AND (?5 = 0 OR coalesce(CAST(json_extract(" + SAFE_ENCHANT_OBJECT +
                    ", '$.level') AS INTEGER), 0) >= ?5) " +
                    "     AND (?6 = 0 OR coalesce(CAST(json_extract(" + SAFE_ENCHANT_OBJECT +
                    ", '$.level') AS INTEGER), 0) = ?6))) " +
                    "AND (?7 = 0 " +
                    "     OR (?7 = 1 AND " + IS_BOOK + ") " +
                    "     OR (?7 = 2 AND " + IS_ENCHANTED_BOOK + ") " +
                    "     OR (?7 = 3 AND " + HAS_ENCHANTMENTS + ") " +
                    "     OR (?7 = 4 AND cs.base_material IS NOT NULL " +
                    "         AND (cs.item_details IS NULL OR json_valid(cs.item_details)) " +
                    "         AND NOT " + HAS_ENCHANTMENTS + " AND NOT " + IS_ENCHANTED_BOOK + ")) " +
                    "AND (?8 = 0 OR cs.is_buy_sign = 1) " +
                    "AND (?8 = 1 OR cs.is_sell_sign = 1) " +
                    "AND (?9 = '' OR cs.server = ?9) " +
                    "AND (?10 = 0 OR cs.is_full = 0) " +
                    "AND (?11 = 0 OR cs.quantity_available > 0) " +
                    "AND cs.is_hidden = 0 ";

    public ShopRepository(Db db) {
        this.db = db;
    }

    private static String orderBy(SortBy sortBy, TradeType tradeType) {
        if (sortBy == SortBy.BEST_PRICE && tradeType == TradeType.BUY)
            return "ORDER BY (cs.buy_price_each IS NULL), cs.buy_price_each ASC ";
        if (sortBy == SortBy.BEST_PRICE && tradeType == TradeType.SELL)
            return "ORDER BY (cs.sell_price_each IS NOT NULL), cs.sell_price_each DESC ";
        if (sortBy == SortBy.QUANTITY_AVAILABLE)
            return "ORDER BY (cs.quantity_available IS NOT NULL), cs.quantity_available DESC ";
        if (sortBy == SortBy.QUANTITY)
            return "ORDER BY (cs.quantity IS NOT NULL), cs.quantity DESC ";
        return "ORDER BY cs.material ASC ";
    }

    private void bindListWhere(PreparedStatement ps, String material, String displayName, String query,
                               String enchantment, int minEnchantmentLevel, int enchantmentLevel,
                               ItemType itemType, TradeType tradeType,
                               String serverStr, boolean hideUnavailable) throws SQLException {
        boolean isBuy = tradeType == TradeType.BUY;
        int itemTypeCode = itemType.queryCode();
        ps.setString(1, material);
        ps.setString(2, displayName);
        ps.setString(3, query);
        ps.setString(4, enchantment);
        ps.setInt(5, minEnchantmentLevel);
        ps.setInt(6, enchantmentLevel);
        ps.setInt(7, itemTypeCode);
        ps.setInt(8, isBuy ? 1 : 0);
        ps.setString(9, serverStr);
        ps.setInt(10, hideUnavailable && tradeType == TradeType.SELL ? 1 : 0);
        ps.setInt(11, hideUnavailable && tradeType == TradeType.BUY ? 1 : 0);
    }

    public List<ChestShopRow> find(String material, String displayName, String query, String enchantment,
                                   int minEnchantmentLevel, int enchantmentLevel,
                                   ItemType itemType, TradeType tradeType,
                                   String serverStr,
                                   boolean hideUnavailable, SortBy sortBy, Integer limit, Integer offset) throws SQLException {
        String sql = SELECT + LIST_WHERE + orderBy(sortBy, tradeType);
        if (limit != null) sql += "LIMIT " + limit + " OFFSET " + offset;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                bindListWhere(ps, material, displayName, query, enchantment, minEnchantmentLevel, enchantmentLevel,
                        itemType, tradeType, serverStr, hideUnavailable);
                return mapRows(ps.executeQuery());
            }
        }
    }

    public long count(String material, String displayName, String query, String enchantment,
                      int minEnchantmentLevel, int enchantmentLevel,
                      ItemType itemType, TradeType tradeType, String serverStr,
                      boolean hideUnavailable) throws SQLException {
        String sql = "SELECT COUNT(*) FROM chest_shop_sign cs " + LIST_WHERE;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                bindListWhere(ps, material, displayName, query, enchantment, minEnchantmentLevel, enchantmentLevel,
                        itemType, tradeType, serverStr, hideUnavailable);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            }
        }
    }

    /** Backward-compatible overload for callers using only a minimum level. */
    public List<ChestShopRow> find(String material, String displayName, String query, String enchantment,
                                   int minEnchantmentLevel, ItemType itemType, TradeType tradeType,
                                   String serverStr, boolean hideUnavailable, SortBy sortBy,
                                   Integer limit, Integer offset) throws SQLException {
        return find(material, displayName, query, enchantment, minEnchantmentLevel, 0,
                itemType, tradeType, serverStr, hideUnavailable, sortBy, limit, offset);
    }

    /** Backward-compatible overload for callers using only a minimum level. */
    public long count(String material, String displayName, String query, String enchantment,
                      int minEnchantmentLevel, ItemType itemType, TradeType tradeType, String serverStr,
                      boolean hideUnavailable) throws SQLException {
        return count(material, displayName, query, enchantment, minEnchantmentLevel, 0,
                itemType, tradeType, serverStr, hideUnavailable);
    }

    /**
     * Distinct plain display names of visible shops, for the search dropdown.
     * Case-insensitively deduplicated, keeping the first-seen casing.
     */
    public List<String> distinctDisplayNames(TradeType tradeType, String serverStr) throws SQLException {
        boolean isBuy = tradeType == TradeType.BUY;
        String sql = "SELECT DISTINCT display_name_plain FROM chest_shop_sign " +
                "WHERE display_name_plain IS NOT NULL " +
                "AND is_hidden = 0 " +
                "AND (? = '' OR server = ?) " +
                "AND (? = 0 OR is_buy_sign = 1) " +
                "AND (? = 1 OR is_sell_sign = 1) " +
                "ORDER BY display_name_plain COLLATE NOCASE";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
                ps.setInt(3, isBuy ? 1 : 0);
                ps.setInt(4, isBuy ? 1 : 0);
                List<String> result = new ArrayList<>();
                java.util.Set<String> seen = new java.util.HashSet<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String name = rs.getString(1);
                        if (seen.add(name.toLowerCase(java.util.Locale.ROOT))) result.add(name);
                    }
                }
                return result;
            }
        }
    }

    public List<String> distinctMaterialNames(TradeType tradeType, String serverStr) throws SQLException {
        boolean isBuy = tradeType == TradeType.BUY;
        String sql = "SELECT DISTINCT material FROM chest_shop_sign " +
                "WHERE is_hidden = 0 " +
                "AND (? = '' OR server = ?) " +
                "AND (? = 0 OR is_buy_sign = 1) " +
                "AND (? = 1 OR is_sell_sign = 1) " +
                "ORDER BY material";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
                ps.setInt(3, isBuy ? 1 : 0);
                ps.setInt(4, isBuy ? 1 : 0);
                List<String> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(rs.getString(1));
                }
                return result;
            }
        }
    }

    /** Distinct searchable enchantment keys, e.g. "fire_aspect". */
    public List<String> distinctEnchantmentNames(TradeType tradeType, String serverStr) throws SQLException {
        List<String> result = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (EnchantmentOption option : distinctEnchantmentOptions(tradeType, serverStr)) {
            if (seen.add(option.name())) result.add(option.name());
        }
        return result;
    }

    /** Distinct searchable enchantment name/level pairs for search suggestions. */
    public List<EnchantmentOption> distinctEnchantmentOptions(TradeType tradeType, String serverStr) throws SQLException {
        return distinctEnchantmentOptions("", "", "", "", ItemType.ALL,
                tradeType, serverStr, false);
    }

    /**
     * Distinct searchable enchantment name/level pairs from the complete set
     * of rows matching the supplied search context. Exact/minimum levels are
     * deliberately omitted so the caller can present every available level
     * as a result-derived facet.
     */
    public List<EnchantmentOption> distinctEnchantmentOptions(
            String material, String displayName, String query, String enchantment,
            ItemType itemType, TradeType tradeType, String serverStr,
            boolean hideUnavailable) throws SQLException {
        String sql = "SELECT DISTINCT lower(json_extract(" + SAFE_FACET_ENCHANT_OBJECT +
                ", '$.name')) AS name, " +
                "CAST(json_extract(" + SAFE_FACET_ENCHANT_OBJECT + ", '$.level') AS INTEGER) AS level " +
                "FROM chest_shop_sign cs, " +
                SEARCH_ENCHANTS + " facet_enchant " +
                LIST_WHERE +
                "AND facet_enchant.type = 'object' " +
                "AND json_type(" + SAFE_FACET_ENCHANT_OBJECT + ", '$.name') = 'text' " +
                "AND trim(json_extract(" + SAFE_FACET_ENCHANT_OBJECT + ", '$.name')) <> '' " +
                "AND CAST(json_extract(" + SAFE_FACET_ENCHANT_OBJECT + ", '$.level') AS INTEGER) > 0 " +
                "ORDER BY name, level";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                bindListWhere(ps, material, displayName, query, enchantment,
                        0, 0, itemType, tradeType, serverStr, hideUnavailable);
                List<EnchantmentOption> result = new ArrayList<>();
                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(new EnchantmentOption(rs.getString(1), rs.getInt(2)));
                }
                return result;
            }
        }
    }

    public record EnchantmentOption(String name, int level) {
    }

    private static final String OWNED_WHERE =
            "WHERE cs.owner_id = ? AND cs.is_hidden = 0 " +
                    "AND (? = 0 OR cs.is_buy_sign = 1) " +
                    "AND (? = 1 OR cs.is_sell_sign = 1) ";

    public List<ChestShopRow> findOwnedBy(long ownerId, TradeType tradeType, Integer limit, Integer offset) throws SQLException {
        String sql = SELECT + OWNED_WHERE + "ORDER BY cs.material ASC ";
        if (limit != null) sql += "LIMIT " + limit + " OFFSET " + offset;
        boolean isBuy = tradeType == TradeType.BUY;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, ownerId);
                ps.setInt(2, isBuy ? 1 : 0);
                ps.setInt(3, isBuy ? 1 : 0);
                return mapRows(ps.executeQuery());
            }
        }
    }

    public long countOwnedBy(long ownerId, TradeType tradeType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM chest_shop_sign cs " + OWNED_WHERE;
        boolean isBuy = tradeType == TradeType.BUY;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, ownerId);
                ps.setInt(2, isBuy ? 1 : 0);
                ps.setInt(3, isBuy ? 1 : 0);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            }
        }
    }

    private static final String IN_REGION_WHERE =
            "WHERE cs.town_id = ? AND cs.is_hidden = 0 " +
                    "AND (? = 0 OR cs.is_buy_sign = 1) " +
                    "AND (? = 1 OR cs.is_sell_sign = 1) ";

    public List<ChestShopRow> findInRegion(long townId, TradeType tradeType, Integer limit, Integer offset) throws SQLException {
        String sql = SELECT + IN_REGION_WHERE + "ORDER BY cs.material ASC ";
        if (limit != null) sql += "LIMIT " + limit + " OFFSET " + offset;
        boolean isBuy = tradeType == TradeType.BUY;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, townId);
                ps.setInt(2, isBuy ? 1 : 0);
                ps.setInt(3, isBuy ? 1 : 0);
                return mapRows(ps.executeQuery());
            }
        }
    }

    /** All shops currently associated with a location, including hidden rows. */
    public List<ChestShopRow> findAssignedToRegion(long townId) throws SQLException {
        String sql = SELECT + "WHERE cs.town_id = ?";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, townId);
                return mapRows(ps.executeQuery());
            }
        }
    }

    public long countInRegion(long townId, TradeType tradeType) throws SQLException {
        String sql = "SELECT COUNT(*) FROM chest_shop_sign cs " + IN_REGION_WHERE;
        boolean isBuy = tradeType == TradeType.BUY;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setLong(1, townId);
                ps.setInt(2, isBuy ? 1 : 0);
                ps.setInt(3, isBuy ? 1 : 0);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
            }
        }
    }

    public List<ChestShopRow> findVisible(String serverStr) throws SQLException {
        String sql = SELECT + "WHERE cs.is_hidden = 0 AND (? = '' OR cs.server = ?)";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverStr);
                ps.setString(2, serverStr);
                return mapRows(ps.executeQuery());
            }
        }
    }

    // Mirrors ChestShop.findInLocation: no hidden filter, server bound by enum name.
    public List<ChestShopRow> findInBounds(String serverEnumName, int lx, int ux, int ly, int uy, int lz, int uz) throws SQLException {
        String sql = SELECT + "WHERE cs.server = ? " +
                "AND ? <= cs.x AND ? >= cs.x " +
                "AND ? <= cs.y AND ? >= cs.y " +
                "AND ? <= cs.z AND ? >= cs.z";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, serverEnumName);
                ps.setInt(2, lx);
                ps.setInt(3, ux);
                ps.setInt(4, ly);
                ps.setInt(5, uy);
                ps.setInt(6, lz);
                ps.setInt(7, uz);
                return mapRows(ps.executeQuery());
            }
        }
    }

    public void setTownAndHidden(String shopId, Long townId, boolean hidden) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "UPDATE chest_shop_sign SET town_id = ?, is_hidden = ? WHERE id = ?")) {
                if (townId == null) ps.setNull(1, java.sql.Types.INTEGER);
                else ps.setLong(1, townId);
                ps.setInt(2, hidden ? 1 : 0);
                ps.setString(3, shopId);
                ps.executeUpdate();
            }
        }
    }

    public void deleteById(String id) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement("DELETE FROM chest_shop_sign WHERE id = ?")) {
                ps.setString(1, id);
                ps.executeUpdate();
            }
        }
    }

    public boolean exists(String id) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement("SELECT 1 FROM chest_shop_sign WHERE id = ?")) {
                ps.setString(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next();
                }
            }
        }
    }

    /**
     * Insert or update. On update, server/x/y/z are left untouched — the previous
     * backend only set them when creating a new row (the id encodes the location,
     * so they never change for an existing shop).
     */
    public void upsert(ChestShopRow row) throws SQLException {
        // base_material/item_details: an event that couldn't resolve the item
        // (base_material null) keeps whatever details the row already has; a
        // resolved event is authoritative, including clearing details when the
        // item has none.
        String sql = "INSERT INTO chest_shop_sign " +
                "(id, server, x, y, z, material, owner_id, town_id, quantity, quantity_available, " +
                "buy_price, sell_price, buy_price_each, sell_price_each, is_full, is_hidden, is_buy_sign, is_sell_sign, " +
                "base_material, item_details, display_name_plain) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "material = excluded.material, owner_id = excluded.owner_id, town_id = excluded.town_id, " +
                "quantity = excluded.quantity, quantity_available = excluded.quantity_available, " +
                "buy_price = excluded.buy_price, sell_price = excluded.sell_price, " +
                "buy_price_each = excluded.buy_price_each, sell_price_each = excluded.sell_price_each, " +
                "is_full = excluded.is_full, is_hidden = excluded.is_hidden, " +
                "is_buy_sign = excluded.is_buy_sign, is_sell_sign = excluded.is_sell_sign, " +
                "base_material = COALESCE(excluded.base_material, chest_shop_sign.base_material), " +
                "item_details = CASE WHEN excluded.base_material IS NULL " +
                "THEN chest_shop_sign.item_details ELSE excluded.item_details END, " +
                "display_name_plain = CASE WHEN excluded.base_material IS NULL " +
                "THEN chest_shop_sign.display_name_plain ELSE excluded.display_name_plain END";
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                ps.setString(1, row.id);
                ps.setString(2, row.server);
                ps.setInt(3, row.x);
                ps.setInt(4, row.y);
                ps.setInt(5, row.z);
                ps.setString(6, row.material);
                setNullableLong(ps, 7, row.ownerId);
                setNullableLong(ps, 8, row.townId);
                setNullableInt(ps, 9, row.quantity);
                setNullableInt(ps, 10, row.quantityAvailable);
                setNullableDouble(ps, 11, row.buyPrice);
                setNullableDouble(ps, 12, row.sellPrice);
                setNullableDouble(ps, 13, row.buyPriceEach);
                setNullableDouble(ps, 14, row.sellPriceEach);
                setNullableBool(ps, 15, row.isFull);
                setNullableBool(ps, 16, row.isHidden);
                setNullableBool(ps, 17, row.isBuySign);
                setNullableBool(ps, 18, row.isSellSign);
                ps.setString(19, row.baseMaterial);
                ps.setString(20, row.itemDetails);
                ps.setString(21, row.displayNamePlain);
                ps.executeUpdate();
            }
        }
    }

    /** Every shop's id and location (including hidden ones) — used by the rescanner. */
    public List<ShopCoord> findAllCoords() throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "SELECT id, x, y, z FROM chest_shop_sign");
                 ResultSet rs = ps.executeQuery()) {
                List<ShopCoord> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new ShopCoord(rs.getString("id"), rs.getInt("x"), rs.getInt("y"), rs.getInt("z")));
                }
                return result;
            }
        }
    }

    public static class ShopCoord {
        public final String id;
        public final int x, y, z;

        public ShopCoord(String id, int x, int y, int z) {
            this.id = id;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    static void setNullableLong(PreparedStatement ps, int i, Long v) throws SQLException {
        if (v == null) ps.setNull(i, java.sql.Types.INTEGER);
        else ps.setLong(i, v);
    }

    static void setNullableInt(PreparedStatement ps, int i, Integer v) throws SQLException {
        if (v == null) ps.setNull(i, java.sql.Types.INTEGER);
        else ps.setInt(i, v);
    }

    static void setNullableDouble(PreparedStatement ps, int i, Double v) throws SQLException {
        if (v == null) ps.setNull(i, java.sql.Types.REAL);
        else ps.setDouble(i, v);
    }

    static void setNullableBool(PreparedStatement ps, int i, Boolean v) throws SQLException {
        if (v == null) ps.setNull(i, java.sql.Types.INTEGER);
        else ps.setInt(i, v ? 1 : 0);
    }

    static Integer getNullableInt(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v;
    }

    static Long getNullableLong(ResultSet rs, String col) throws SQLException {
        long v = rs.getLong(col);
        return rs.wasNull() ? null : v;
    }

    static Double getNullableDouble(ResultSet rs, String col) throws SQLException {
        double v = rs.getDouble(col);
        return rs.wasNull() ? null : v;
    }

    static Boolean getNullableBool(ResultSet rs, String col) throws SQLException {
        int v = rs.getInt(col);
        return rs.wasNull() ? null : v != 0;
    }

    private static List<ChestShopRow> mapRows(ResultSet rs) throws SQLException {
        List<ChestShopRow> result = new ArrayList<>();
        try (rs) {
            while (rs.next()) {
                ChestShopRow row = new ChestShopRow();
                row.id = rs.getString("id");
                row.server = rs.getString("server");
                row.x = rs.getInt("x");
                row.y = rs.getInt("y");
                row.z = rs.getInt("z");
                row.material = rs.getString("material");
                row.ownerId = getNullableLong(rs, "owner_id");
                row.townId = getNullableLong(rs, "town_id");
                row.quantity = getNullableInt(rs, "quantity");
                row.quantityAvailable = getNullableInt(rs, "quantity_available");
                row.buyPrice = getNullableDouble(rs, "buy_price");
                row.sellPrice = getNullableDouble(rs, "sell_price");
                row.buyPriceEach = getNullableDouble(rs, "buy_price_each");
                row.sellPriceEach = getNullableDouble(rs, "sell_price_each");
                row.isFull = getNullableBool(rs, "is_full");
                row.isHidden = getNullableBool(rs, "is_hidden");
                row.isBuySign = getNullableBool(rs, "is_buy_sign");
                row.isSellSign = getNullableBool(rs, "is_sell_sign");
                row.baseMaterial = rs.getString("base_material");
                row.itemDetails = rs.getString("item_details");
                row.displayNamePlain = rs.getString("display_name_plain");
                row.ownerName = rs.getString("owner_name");
                row.townName = rs.getString("town_name");
                row.townType = ShopLocationType.fromString(rs.getString("town_type"));
                result.add(row);
            }
        }
        return result;
    }
}
