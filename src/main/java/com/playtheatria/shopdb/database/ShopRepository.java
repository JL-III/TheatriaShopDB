package com.playtheatria.shopdb.database;

import com.playtheatria.shopdb.models.SortBy;
import com.playtheatria.shopdb.models.TradeType;

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

    private static final String SELECT =
            "SELECT cs.id, cs.server, cs.x, cs.y, cs.z, cs.material, cs.owner_id, cs.town_id, " +
                    "cs.quantity, cs.quantity_available, cs.buy_price, cs.sell_price, " +
                    "cs.buy_price_each, cs.sell_price_each, cs.is_full, cs.is_hidden, " +
                    "cs.is_buy_sign, cs.is_sell_sign, cs.base_material, cs.item_details, " +
                    "p.name AS owner_name, r.name AS town_name " +
                    "FROM chest_shop_sign cs " +
                    "LEFT JOIN player p ON p.id = cs.owner_id " +
                    "LEFT JOIN region r ON r.id = cs.town_id ";

    private static final String LIST_WHERE =
            "WHERE (? = '' OR cs.material = ?) " +
                    "AND (? = 0 OR cs.is_buy_sign = 1) " +
                    "AND (? = 1 OR cs.is_sell_sign = 1) " +
                    "AND (? = '' OR cs.server = ?) " +
                    "AND (? = 0 OR cs.is_full = 0) " +
                    "AND (? = 0 OR cs.quantity_available > 0) " +
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

    private void bindListWhere(PreparedStatement ps, String material, TradeType tradeType,
                               String serverStr, boolean hideUnavailable) throws SQLException {
        boolean isBuy = tradeType == TradeType.BUY;
        ps.setString(1, material);
        ps.setString(2, material);
        ps.setInt(3, isBuy ? 1 : 0);
        ps.setInt(4, isBuy ? 1 : 0);
        ps.setString(5, serverStr);
        ps.setString(6, serverStr);
        ps.setInt(7, hideUnavailable && tradeType == TradeType.SELL ? 1 : 0);
        ps.setInt(8, hideUnavailable && tradeType == TradeType.BUY ? 1 : 0);
    }

    public List<ChestShopRow> find(String material, TradeType tradeType, String serverStr,
                                   boolean hideUnavailable, SortBy sortBy, Integer limit, Integer offset) throws SQLException {
        String sql = SELECT + LIST_WHERE + orderBy(sortBy, tradeType);
        if (limit != null) sql += "LIMIT " + limit + " OFFSET " + offset;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                bindListWhere(ps, material, tradeType, serverStr, hideUnavailable);
                return mapRows(ps.executeQuery());
            }
        }
    }

    public long count(String material, TradeType tradeType, String serverStr, boolean hideUnavailable) throws SQLException {
        String sql = "SELECT COUNT(*) FROM chest_shop_sign cs " + LIST_WHERE;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(sql)) {
                bindListWhere(ps, material, tradeType, serverStr, hideUnavailable);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getLong(1) : 0;
                }
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
                "base_material, item_details) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET " +
                "material = excluded.material, owner_id = excluded.owner_id, town_id = excluded.town_id, " +
                "quantity = excluded.quantity, quantity_available = excluded.quantity_available, " +
                "buy_price = excluded.buy_price, sell_price = excluded.sell_price, " +
                "buy_price_each = excluded.buy_price_each, sell_price_each = excluded.sell_price_each, " +
                "is_full = excluded.is_full, is_hidden = excluded.is_hidden, " +
                "is_buy_sign = excluded.is_buy_sign, is_sell_sign = excluded.is_sell_sign, " +
                "base_material = COALESCE(excluded.base_material, chest_shop_sign.base_material), " +
                "item_details = CASE WHEN excluded.base_material IS NULL " +
                "THEN chest_shop_sign.item_details ELSE excluded.item_details END";
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
                row.ownerName = rs.getString("owner_name");
                row.townName = rs.getString("town_name");
                result.add(row);
            }
        }
        return result;
    }
}
