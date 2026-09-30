package com.veldin.tokens;

/**
 * Six-bit token encoding used by the initial language models.
 *
 * This is deliberately a "good enough" character set for the first version.
 * It covers the characters I expect to be useful across the Western European
 * languages that are most relevant to me, while keeping the number of tokens
 * small enough to fit comfortably in 6 bits.
 *
 * There is a trade-off here between the number of characters we support and
 * how much meaning each token has. More tokens give better coverage for
 * individual languages, but leave fewer values available for other purposes.
 *
 * For now, I prefer one shared encoding that works reasonably well across
 * these languages. In the future, I may split this into language-specific
 * token sets, allowing each language to make better use of its 64 available
 * values.
 */

public final class SixBitWordToken implements TokenCodec {

    public static final SixBitWordToken instance = new SixBitWordToken();

    public static final int TOKEN_BITS = 6;
    public static final int TOKEN_MASK = 0x3F;

    public static final int NULL_TOKEN = 0;
    public static final int UNKNOWN_TOKEN = 63;

    // Used for sentences
    public static final int WORD_SEPARATOR_TOKEN = 27;
    public static final int SENTENCE_END_TOKEN = 62;

    // Used for Vocab
    public static final int FIRST_CHARACTER_TOKEN = 1;
    public static final int LAST_CHARACTER_TOKEN = 48;


    private SixBitWordToken() {

    }

    @Override
    public int toToken(int codePoint) {
        return switch (codePoint) {
            case '\0' -> 0;

            case 'a', 'A' -> 1;
            case 'b', 'B' -> 2;
            case 'c', 'C' -> 3;
            case 'd', 'D' -> 4;
            case 'e', 'E' -> 5;
            case 'f', 'F' -> 6;
            case 'g', 'G' -> 7;
            case 'h', 'H' -> 8;
            case 'i', 'I' -> 9;
            case 'j', 'J' -> 10;
            case 'k', 'K' -> 11;
            case 'l', 'L' -> 12;
            case 'm', 'M' -> 13;
            case 'n', 'N' -> 14;
            case 'o', 'O' -> 15;
            case 'p', 'P' -> 16;
            case 'q', 'Q' -> 17;
            case 'r', 'R' -> 18;
            case 's', 'S' -> 19;
            case 't', 'T' -> 20;
            case 'u', 'U' -> 21;
            case 'v', 'V' -> 22;
            case 'w', 'W' -> 23;
            case 'x', 'X' -> 24;
            case 'y', 'Y' -> 25;
            case 'z', 'Z' -> 26;

            // Connectors between 'words'
            case ' ', '-', '_', ',' -> WORD_SEPARATOR_TOKEN; // 27

            // Dutch / Flemish / Belgian
            case 'é', 'É' -> 28;
            case 'è', 'È' -> 29;
            case 'ë', 'Ë' -> 30;
            case 'ï', 'Ï' -> 31;
            case 'ö', 'Ö' -> 32;
            case 'ü', 'Ü' -> 33;

            // Other common Dutch/Flemish accents
            case 'á', 'Á' -> 34;
            case 'à', 'À' -> 35;
            case 'ó', 'Ó' -> 36;
            case 'ò', 'Ò' -> 37;
            case 'ú', 'Ú' -> 38;

            // Common Belgian / French characters
            case 'ç', 'Ç' -> 39;
            case 'ê', 'Ê' -> 40;
            case 'â', 'Â' -> 41;
            case 'î', 'Î' -> 42;
            case 'ô', 'Ô' -> 43;
            case 'û', 'Û' -> 44;

            // Common European letters
            case 'ñ', 'Ñ' -> 45;
            case 'ß' -> 46;
            case 'æ', 'Æ' -> 47;
            case 'œ', 'Œ' -> 48;

            // TODO; deside what to do with these
            // case 'ù', 'Ù' -> 49;
            // case '\'', '’' -> 50;

            // Ends of (part of) sentences (as strings of words)
            case '.' -> SENTENCE_END_TOKEN; // 62

            default -> 63;
        };

    }

    @Override
    public int toCodePoint(int token) {
        return switch (token) {
            case 0 -> '\0';

            case 1 -> 'a';
            case 2 -> 'b';
            case 3 -> 'c';
            case 4 -> 'd';
            case 5 -> 'e';
            case 6 -> 'f';
            case 7 -> 'g';
            case 8 -> 'h';
            case 9 -> 'i';
            case 10 -> 'j';
            case 11 -> 'k';
            case 12 -> 'l';
            case 13 -> 'm';
            case 14 -> 'n';
            case 15 -> 'o';
            case 16 -> 'p';
            case 17 -> 'q';
            case 18 -> 'r';
            case 19 -> 's';
            case 20 -> 't';
            case 21 -> 'u';
            case 22 -> 'v';
            case 23 -> 'w';
            case 24 -> 'x';
            case 25 -> 'y';
            case 26 -> 'z';

            case WORD_SEPARATOR_TOKEN -> ' '; // 27 - Word separator

            // Dutch / Flemish / Belgian
            case 28 -> 'é';
            case 29 -> 'è';
            case 30 -> 'ë';
            case 31 -> 'ï';
            case 32 -> 'ö';
            case 33 -> 'ü';

            // Other common Dutch/Flemish accents
            case 34 -> 'á';
            case 35 -> 'à';
            case 36 -> 'ó';
            case 37 -> 'ò';
            case 38 -> 'ú';

            // Common Belgian / French characters
            case 39 -> 'ç';
            case 40 -> 'ê';
            case 41 -> 'â';
            case 42 -> 'î';
            case 43 -> 'ô';
            case 44 -> 'û';

            // Common European letters
            case 45 -> 'ñ';
            case 46 -> 'ß';
            case 47 -> 'æ';
            case 48 -> 'œ';

            case SENTENCE_END_TOKEN -> '.'; // 62

            case 63 -> '?'; // UNKNOWN_TOKEN

            default -> throw new IllegalArgumentException("Invalid token: " + token);
        };
    }

    @Override
    public int getTokenMask() {
        return TOKEN_MASK;
    }

}