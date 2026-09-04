package com.playtheatria.shopdb.updater;

import com.playtheatria.shopdb.models.EventType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class EventBufferTest {

    @TempDir
    Path tempDir;

    @Test
    void acknowledgeRemovesOnlyUploadedRevisions() throws Exception {
        EventBuffer buffer = new EventBuffer(tempDir.resolve("events.db").toFile(),
                Logger.getAnonymousLogger());
        try {
            buffer.createOrUpdate(event("same-id", "old"));
            buffer.createOrUpdate(event("unchanged-id", "unchanged"));
            List<BufferedShopEvent> uploaded = buffer.findAll();

            buffer.createOrUpdate(event("same-id", "new"));
            buffer.createOrUpdate(event("post-snapshot-id", "post-snapshot"));
            buffer.acknowledge(uploaded);

            Map<String, BufferedShopEvent> remaining = byId(buffer.findAll());
            assertEquals(Set.of("same-id", "post-snapshot-id"), remaining.keySet());
            assertEquals("new", remaining.get("same-id").owner);
            assertEquals(2, buffer.count());

            buffer.createOrUpdate(event("after-ack-id", "after-ack"));
            assertEquals(3, buffer.count());
            assertEquals("after-ack", byId(buffer.findAll()).get("after-ack-id").owner);
        } finally {
            buffer.close();
        }
    }

    @Test
    void reusedEventInstanceCannotChangeAnEarlierSnapshotRevision() throws Exception {
        EventBuffer buffer = new EventBuffer(tempDir.resolve("reused-event.db").toFile(),
                Logger.getAnonymousLogger());
        try {
            BufferedShopEvent reused = event("same-id", "old");
            buffer.createOrUpdate(reused);
            List<BufferedShopEvent> uploaded = buffer.findAll();
            String uploadedRevision = uploaded.get(0).bufferRevision;

            reused.owner = "new";
            buffer.createOrUpdate(reused);
            assertNotEquals(uploadedRevision, reused.bufferRevision);

            buffer.acknowledge(uploaded);
            BufferedShopEvent remaining = byId(buffer.findAll()).get("same-id");
            assertNotNull(remaining);
            assertEquals("new", remaining.owner);
        } finally {
            buffer.close();
        }
    }

    @Test
    void migratesAndAcknowledgesLegacyRowsWithoutARevision() throws Exception {
        File databaseFile = tempDir.resolve("legacy-events.db").toFile();
        EventBuffer current = new EventBuffer(databaseFile, Logger.getAnonymousLogger());
        current.close();

        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + databaseFile.getAbsolutePath());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("ALTER TABLE shop_events DROP COLUMN bufferRevision");
            statement.executeUpdate("INSERT INTO shop_events (id, owner) VALUES ('legacy-id', 'legacy')");
        }

        EventBuffer migrated = new EventBuffer(databaseFile, Logger.getAnonymousLogger());
        try {
            List<BufferedShopEvent> uploaded = migrated.findAll();
            assertEquals(1, uploaded.size());
            assertNotNull(uploaded.get(0).bufferRevision);

            migrated.acknowledge(uploaded);
            assertEquals(0, migrated.count());
        } finally {
            migrated.close();
        }
    }

    private static BufferedShopEvent event(String id, String owner) {
        BufferedShopEvent event = new BufferedShopEvent();
        event.id = id;
        event.eventType = EventType.UPDATE;
        event.owner = owner;
        return event;
    }

    private static Map<String, BufferedShopEvent> byId(List<BufferedShopEvent> events) {
        return events.stream().collect(Collectors.toMap(event -> event.id, Function.identity()));
    }
}
