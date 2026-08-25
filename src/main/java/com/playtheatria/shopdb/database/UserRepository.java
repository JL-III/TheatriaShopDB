package com.playtheatria.shopdb.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private final Db db;

    public UserRepository(Db db) {
        this.db = db;
    }

    /** Returns the bcrypt password hash for the given username, or null if no such user. */
    public String findPasswordByUsername(String username) throws SQLException {
        if (username == null) return null;
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "SELECT password FROM users WHERE username = ?")) {
                ps.setString(1, username);
                try (ResultSet rs = ps.executeQuery()) {
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        }
    }
}
