package com.playtheatria.shopdb.updater;

import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.DaoManager;
import com.j256.ormlite.dao.GenericRawResults;
import com.j256.ormlite.db.SqliteDatabaseType;
import com.j256.ormlite.jdbc.JdbcConnectionSource;
import com.j256.ormlite.misc.TransactionManager;
import com.j256.ormlite.support.ConnectionSource;
import com.j256.ormlite.table.TableUtils;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The persistent event buffer (ShopDB-Updater's EventRepository/DaoCreator):
 * events are written here as they happen and acknowledged only after ShopDB
 * confirms a successful POST, so nothing is lost across restarts or outages.
 */
public class EventBuffer {
    private final Logger logger;
    private final ConnectionSource connectionSource;
    private final Dao<BufferedShopEvent, String> dao;

    public EventBuffer(File databaseFile, Logger logger) throws SQLException {
        this.logger = logger;
        this.connectionSource = new JdbcConnectionSource("jdbc:sqlite:" + databaseFile.getAbsolutePath(), new SqliteDatabaseType());
        this.dao = DaoManager.createDao(connectionSource, BufferedShopEvent.class);
        TableUtils.createTableIfNotExists(connectionSource, BufferedShopEvent.class);
        addColumnIfMissing("baseMaterial");
        addColumnIfMissing("itemDetails");
        addColumnIfMissing("bufferRevision");
        initializeMissingRevisions();
    }

    /** Adds newer columns to a pre-existing buffer database. */
    private void addColumnIfMissing(String column) throws SQLException {
        try (GenericRawResults<String[]> columns = dao.queryRaw("PRAGMA table_info(shop_events)")) {
            for (String[] existing : columns.getResults()) {
                if (existing.length > 1 && column.equalsIgnoreCase(existing[1])) return;
            }
        } catch (IOException e) {
            throw new SQLException("Failed to inspect shop event buffer schema", e);
        }
        dao.executeRaw("ALTER TABLE shop_events ADD COLUMN `" + column + "` VARCHAR");
    }

    /** Makes buffered rows from older versions safe to acknowledge conditionally. */
    private void initializeMissingRevisions() throws SQLException {
        dao.updateRaw("UPDATE shop_events SET `bufferRevision` = ? WHERE `bufferRevision` IS NULL",
                UUID.randomUUID().toString());
    }

    public synchronized List<BufferedShopEvent> findAll() {
        try {
            return dao.queryForAll();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to read shop event buffer", e);
            return null;
        }
    }

    public synchronized long count() {
        try {
            return dao.countOf();
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to count shop event buffer", e);
            return 0;
        }
    }

    public synchronized void createOrUpdate(BufferedShopEvent event) {
        try {
            event.bufferRevision = UUID.randomUUID().toString();
            dao.createOrUpdate(event);
        } catch (SQLException e) {
            logger.log(Level.WARNING, "Failed to buffer shop event " + event.id, e);
        }
    }

    /**
     * Removes only the exact event revisions included in a successful upload.
     * Writes that arrive after the upload snapshot have a different revision
     * (or a different id) and therefore remain buffered for the next upload.
     */
    public synchronized void acknowledge(List<BufferedShopEvent> uploadedEvents) {
        if (uploadedEvents == null || uploadedEvents.isEmpty()) return;

        try {
            TransactionManager.callInTransaction(connectionSource, () -> {
                for (BufferedShopEvent event : uploadedEvents) {
                    if (event.bufferRevision == null) {
                        dao.executeRaw("DELETE FROM shop_events WHERE id = ? AND bufferRevision IS NULL",
                                event.id);
                    } else {
                        dao.executeRaw("DELETE FROM shop_events WHERE id = ? AND bufferRevision = ?",
                                event.id, event.bufferRevision);
                    }
                }
                return null;
            });
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to acknowledge submitted shop events", e);
        }
    }

    public synchronized void close() {
        try {
            connectionSource.close();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to close shop event buffer", e);
        }
    }
}
