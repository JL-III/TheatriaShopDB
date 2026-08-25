package com.playtheatria.shopdb.models;

// timestamp is epoch millis: the previous backend serialized ErrorResponse with a
// plain ObjectMapper, which writes java.sql.Timestamp as a number.
public class ErrorResponse {
    public long timestamp;
    public int status;
    public String error;
    public String message;

    public ErrorResponse(long timestamp, int status, String error, String message) {
        this.timestamp = timestamp;
        this.status = status;
        this.error = error;
        this.message = message;
    }
}
