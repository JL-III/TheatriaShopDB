package com.playtheatria.shopdb.web;

/** Carries an HTTP status + message; rendered as the standard ErrorResponse JSON. */
public class ApiException extends RuntimeException {
    public final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }
}
