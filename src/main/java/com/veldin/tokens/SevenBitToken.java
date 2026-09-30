package com.veldin.tokens;

public final class SevenBitToken implements TokenCodec {

    public static final SevenBitToken instance = new SevenBitToken();

    /*
     * ============================================================
     * 7-BIT TOKEN FORMAT
     * ============================================================
     *
     * 0       = NULL
     * 1..95   = printable ASCII (32..126)
     * 96..126 Unused
     * 127     = UNKNOWN
     *
     * Values 96..126 are currently unused.
     *
     * Every token fits in exactly 7 bits.
     *
     * This means:
     *
     *     9 tokens × 7 bits = 63 bits
     *
     * which fits perfectly inside a Java long.
     */

    public static final int TOKEN_BITS = 7;
    public static final int TOKEN_MASK = 0x7F;

    public static final int NULL_TOKEN = 0;
    public static final int UNKNOWN_TOKEN = 127;

    private static final int FIRST_PRINTABLE_ASCII = 32;
    private static final int LAST_PRINTABLE_ASCII = 126;

    private SevenBitToken() {

    }

    /**
     * Convert a Unicode code point to a 7-bit token.
     * <p>
     * Rules:
     * <p>
     * - All whitespace becomes ASCII space.
     * - Printable ASCII is preserved.
     * - ASCII control characters become UNKNOWN.
     * - Non-ASCII characters become UNKNOWN.
     */
    @Override
    public int toToken(int codePoint) {

        /*
         * Normalize all whitespace to ASCII space.
         */
        if (Character.isWhitespace(codePoint)) {
            return toAsciiToken(' ');
        }

        /*
         * Preserve printable ASCII.
         */
        if (codePoint >= FIRST_PRINTABLE_ASCII
                && codePoint <= LAST_PRINTABLE_ASCII) {

            return toAsciiToken(codePoint);
        }

        /*
         * Control characters and non-ASCII.
         */
        return UNKNOWN_TOKEN;
    }

    /**
     * Convert printable ASCII to a token.
     * <p>
     * ASCII 32 becomes token 1.
     * ASCII 126 becomes token 95.
     */
    private static int toAsciiToken(int codePoint) {
        return (codePoint - FIRST_PRINTABLE_ASCII) + 1;
    }

    /**
     * Convert a token back to a code point.
     */
    public int toCodePoint(int token) {

        if (token == NULL_TOKEN) {
            throw new IllegalArgumentException(
                    "NULL token has no code point"
            );
        }

        if (token == UNKNOWN_TOKEN) {
            return '?';
        }

        /*
         * Printable ASCII tokens are 1..95.
         */
        if (token >= 1 && token <= 95) {
            return (token - 1) + FIRST_PRINTABLE_ASCII;
        }

        throw new IllegalArgumentException(
                "Invalid token: " + token
        );
    }

    @Override
    public int getTokenMask() {
        return TOKEN_MASK;
    }

    public static boolean isAscii(int token) {
        return token >= 1 && token <= 95;
    }

    public static boolean isUnknown(int token) {
        return token == UNKNOWN_TOKEN;
    }

    public static boolean isNull(int token) {
        return token == NULL_TOKEN;
    }
}