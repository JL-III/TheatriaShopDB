package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.UserRepository;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;
import java.util.logging.Logger;

/**
 * Self-provisions the API user from config: if an api-key is configured, ensure
 * the users table holds a matching bcrypt hash for the api-username. When the
 * stored hash already matches the configured key, nothing is written — so a
 * hash imported from the production dump is left untouched as long as the keys
 * agree.
 */
public final class ApiUserProvisioner {
    public static void provision(UserRepository users, String username, String apiKey, Logger logger) throws SQLException {
        if (apiKey == null || apiKey.isEmpty()) {
            return; // no key configured: the users table is managed externally (e.g. imported dump)
        }

        String existing = users.findPasswordByUsername(username);
        if (existing != null) {
            try {
                if (BCrypt.checkpw(apiKey, existing)) {
                    return; // already in sync
                }
            } catch (IllegalArgumentException ignored) {
                // stored hash is not valid bcrypt; rewrite it below
            }
        }

        users.upsertUser(username, BCrypt.hashpw(apiKey, BCrypt.gensalt()));
        logger.info("Provisioned API user '" + username + "' from config.");
    }

    private ApiUserProvisioner() {
    }
}
