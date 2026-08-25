package com.playtheatria.shopdb.services;

import com.playtheatria.shopdb.database.UserRepository;
import com.playtheatria.shopdb.web.ApiException;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.SQLException;

public class ApiKeyValidator {
    public static final String UNAUTHORIZED = "Unauthorized.";
    public static final String NO_API_USER = "No API user found.";

    private final UserRepository users;
    private final String apiUsername;

    public ApiKeyValidator(UserRepository users, String apiUsername) {
        this.users = users;
        this.apiUsername = apiUsername;
    }

    public void validateAPIKey(String authHeader) throws SQLException {
        if (authHeader == null) {
            throw new ApiException(401, UNAUTHORIZED);
        }

        String token = authHeader.replace("Bearer ", "");

        String passwordHash = users.findPasswordByUsername(apiUsername);
        if (passwordHash == null) {
            throw new ApiException(401, NO_API_USER);
        }

        boolean valid;
        try {
            valid = BCrypt.checkpw(token, passwordHash);
        } catch (IllegalArgumentException e) {
            throw new ApiException(401, UNAUTHORIZED);
        }
        if (!valid) {
            throw new ApiException(401, UNAUTHORIZED);
        }
    }
}
