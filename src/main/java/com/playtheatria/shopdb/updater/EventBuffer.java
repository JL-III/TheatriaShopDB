package com.playtheatria.shopdb.updater;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.db.SqliteDatabaseType;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The persistent event buffer (ShopDB-Updater's EventRepository/DaoCreator):
 * events are written here as they happen and cleared only after ShopDB
 * confirms a successful POST, so nothing is lost across restarts or outages.
 */
public class EventBuffer {
    private final Logger logger;
    private final ConnectionSource connectionSource;
    private final Dao<BufferedShopEvent, String> dao;
    private final SimpleCache<String, BufferedShopEvent> cache;

    public EventBuffer(File databaseFile, int cacheSize, Logger logger) throws SQLException {
        this.logger = logger;
        this.connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + databaseFile.getAbsolutePath(), new SqliteDatabaseType());
        this.dao = DaoManager.createDao(connectionSource, BufferedShopEvent.class);
        TableUtils.createTableIfNotExists(connectionSource, BufferedShopEvent.class);
        this.cache = new SimpleCache<>(cacheSize);
    }

    public List<BufferedShopEvent> findAll() {
        if (count() == cache.size()) {
            return new ArrayList<>(cache.values());
        }
        try {
            return dao.queryForAll();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to read shop event buffer", e);
            return null;
        }
    }

    public long count() {
        try {
            return dao.countOf();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to count shop event buffer", e);
            return 0;
        }
    }

    public void createOrUpdate(BufferedShopEvent event) {
        try {
            dao.createOrUpdate(event);
            cache.put(event.id, event);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to buffer shop event " + event.id, e);
        }
    }

    public void truncate() {
        try {
            TableUtils.dropTable(connectionSource, BufferedShopEvent.class, false);
            TableUtils.createTableIfNotExists(connectionSource, BufferedShopEvent.class);
            cache.clear();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to truncate shop event buffer", e);
        }
    }

    public void close() {
        try {
            connectionSource.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to close shop event buffer", e);
        }
    }
}
