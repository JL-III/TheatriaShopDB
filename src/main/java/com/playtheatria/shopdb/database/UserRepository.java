package com.playtheatria.shopdb.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserRepository {
    private final Db db;

    public UserRepository(Db db) {
        this.db = db;
    }

    /** Inserts or updates the given user's bcrypt password hash. */
    public void upsertUser(String username, String passwordHash) throws SQLException {
        synchronized (db.lock) {
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "UPDATE users SET password = ? WHERE username = ?")) {
                ps.setString(1, passwordHash);
                ps.setString(2, username);
                if (ps.executeUpdate() > 0) return;
            }
            try (PreparedStatement ps = db.connection.prepareStatement(
                    "INSERT INTO users (username, password) VALUES (?, ?)")) {
                ps.setString(1, username);
                ps.setString(2, passwordHash);
                ps.executeUpdate();
            }
        }
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
