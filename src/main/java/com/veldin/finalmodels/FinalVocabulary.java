package com.veldin.finalmodels;

import com.veldin.tokens.SixBitWordToken;

import java.util.HashMap;
import java.util.Map;

public final class FinalVocabulary {

    public static final int VOCABULARY_SIZE = 255;
    public static final int UNKNOWN_TOKEN = 0;
    public static final int MAX_SEQUENCE_LENGTH = 10;

    private final long[] tokenToSequence;
    private final Map<Long, Integer> sequenceToToken;

    public FinalVocabulary(long[] sequences) {

        if (sequences.length != VOCABULARY_SIZE) {
            throw new IllegalArgumentException(
                    "Vocabulary must contain exactly "
                            + VOCABULARY_SIZE
                            + " entries"
            );
        }

        /*
         * Copy the array so the vocabulary cannot
         * accidentally be changed from outside.
         */
        this.tokenToSequence =
                sequences.clone();

        this.sequenceToToken =
                new HashMap<>(VOCABULARY_SIZE);

        /*
         * Build the reverse lookup map.
         */
        for (int token = 0;
             token < VOCABULARY_SIZE;
             token++) {

            long sequence =
                    tokenToSequence[token];

            sequenceToToken.put(
                    sequence,
                    token
            );
        }
    }

    /*
     * ============================================================
     * TOKEN -> SEQUENCE
     * ============================================================
     */

    public long getSequence(int token) {

        if (token < 0 ||
                token >= VOCABULARY_SIZE) {

            throw new IllegalArgumentException(
                    "Invalid vocabulary token: "
                            + token
            );
        }

        return tokenToSequence[token];
    }

    public String getText(int token) {

        if (token < 0 ||
                token >= VOCABULARY_SIZE) {

            throw new IllegalArgumentException(
                    "Invalid vocabulary token: "
                            + token
            );
        }

        return decode(
                tokenToSequence[token]
        );
    }

    /*
     * ============================================================
     * SEQUENCE -> TOKEN
     * ============================================================
     */

    public int getToken(long sequence) {

        return sequenceToToken.getOrDefault(
                sequence,
                UNKNOWN_TOKEN
        );
    }

    /*
     * ============================================================
     * RAW ACCESS
     * ============================================================
     */

    public long[] getSequences() {
        return tokenToSequence.clone();
    }

    /*
     * ============================================================
     * STRING REPRESENTATION
     * ============================================================
     */

    @Override
    public String toString() {

        StringBuilder result =
                new StringBuilder();

        for (int token = 0;
             token < VOCABULARY_SIZE;
             token++) {

            long sequence =
                    tokenToSequence[token];

            result.append("Vocab[")
                    .append(token)
                    .append("] = ")
                    .append(sequence)
                    .append(" // \"")
                    .append(decode(sequence))
                    .append("\"")
                    .append('\n');
        }

        return result.toString();
    }

    /*
     * ============================================================
     * DECODING
     * ============================================================
     */

    private String decode(long sequence) {

        StringBuilder result =
                new StringBuilder(
                        MAX_SEQUENCE_LENGTH
                );

        for (int i = 0;
             i < MAX_SEQUENCE_LENGTH;
             i++) {

            int token =
                    (int) (
                            sequence
                                    & SixBitWordToken.TOKEN_MASK
                    );

            if (token == SixBitWordToken.NULL_TOKEN) {
                break;
            }

            result.appendCodePoint(
                    SixBitWordToken.instance.toCodePoint(token)
            );

            sequence >>=
                    SixBitWordToken.TOKEN_BITS;
        }

        return result
                .reverse()
                .toString();
    }

    @Override
    public int hashCode() {
        return java.util.Arrays.hashCode(tokenToSequence);
    }
}