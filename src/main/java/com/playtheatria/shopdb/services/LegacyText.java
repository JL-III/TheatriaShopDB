package com.playtheatria.shopdb.services;

/** Helpers for legacy §-formatted strings. */
public final class LegacyText {
    private static final char SECTION = '§';

    /**
     * Removes every § formatting code (each § consumes the following character,
     * which also swallows §x hex sequences pair by pair), leaving the plain,
     * trimmed text. Returns null for null input or when nothing remains.
     */
    public static String stripCodes(String text) {
        if (text == null) return null;
        StringBuilder sb = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == SECTION && i + 1 < text.length()) {
                i++;
                continue;
            }
            sb.append(c);
        }
        String plain = sb.toString().trim();
        return plain.isEmpty() ? null : plain;
    }

    private LegacyText() {
    }
}
