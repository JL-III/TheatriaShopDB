package com.playtheatria.shopdb.web;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;

import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public final class Json {
    // Matches the previous backend: nulls omitted, timestamps as ISO strings
    // with an explicit +00:00 offset (e.g. "2026-08-21T04:34:03.111+00:00").
    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .registerTypeAdapter(Timestamp.class,
                    (JsonSerializer<Timestamp>) (src, type, ctx) -> new JsonPrimitive(formatTimestamp(src)))
            .create();

    static String formatTimestamp(Timestamp ts) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'+00:00'");
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf.format(ts);
    }

    private Json() {
    }
}
