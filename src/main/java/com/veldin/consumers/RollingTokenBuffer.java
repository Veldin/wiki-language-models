package com.veldin.consumers;

import com.veldin.finalmodels.FinalVocabulary;
import com.veldin.tokens.SixBitWordToken;

public final class RollingTokenBuffer implements CodePointConsumer {

    private static final int MAX_SEQUENCE_LENGTH =
            FinalVocabulary.MAX_SEQUENCE_LENGTH;

    private final FinalVocabulary vocabulary;
    private final CodePointConsumer child;

    private final int[] buffer =
            new int[MAX_SEQUENCE_LENGTH];

    private int size;

    public RollingTokenBuffer(
            FinalVocabulary vocabulary,
            CodePointConsumer child
    ) {
        this.vocabulary = vocabulary;
        this.child = child;
    }

    @Override
    public void accept(int token) {

        /*
         * UNKNOWN characters are simply ignored.
         *
         * They never enter the rolling buffer and therefore
         * can never become part of a vocabulary sequence.
         */
        if (token == SixBitWordToken.UNKNOWN_TOKEN) {
            return;
        }

        if (token < 0 ||
                token > SixBitWordToken.TOKEN_MASK) {

            throw new IllegalArgumentException(
                    "Invalid SixBit token: " + token
            );
        }

        /*
         * Collapse consecutive word separators.
         *
         * "hello  world" becomes "hello world"
         * "hello   world" becomes "hello world"
         */
        if (token == SixBitWordToken.WORD_SEPARATOR_TOKEN &&
                size > 0 &&
                buffer[size - 1] ==
                        SixBitWordToken.WORD_SEPARATOR_TOKEN) {

            return;
        }

        buffer[size++] = token;

        /*
         * Once the buffer is full, there cannot be a longer
         * vocabulary sequence starting at buffer[0].
         */
        if (size == MAX_SEQUENCE_LENGTH) {
            emitLongest();
        }
    }

    private void emitLongest() {

        /*
         * Greedy longest-match tokenization.
         */
        for (int length = size;
             length > 0;
             length--) {

            long sequence = pack(length);

            int token =
                    vocabulary.getToken(sequence);

            if (token != FinalVocabulary.UNKNOWN_TOKEN) {

                child.accept(token);

                consume(length);

                return;
            }
        }

        /*
         * No multi-character vocabulary entry matched.
         *
         * The first character must itself exist in the
         * vocabulary, because single characters are part of
         * the vocabulary.
         */
        long sequence =
                buffer[0];

        int token =
                vocabulary.getToken(sequence);

        if (token == FinalVocabulary.UNKNOWN_TOKEN) {

            /*
             * This is different from SixBit UNKNOWN.
             *
             * It means the vocabulary does not contain an
             * otherwise valid SixBit character.
             *
             * Ignore it as well.
             */
            consume(1);
            return;
        }

        child.accept(token);
        consume(1);
    }

    private long pack(int length) {

        long sequence = 0L;

        for (int i = 0; i < length; i++) {

            sequence <<= SixBitWordToken.TOKEN_BITS;

            sequence |=
                    buffer[i]
                            & SixBitWordToken.TOKEN_MASK;
        }

        return sequence;
    }

    private void consume(int length) {

        int remaining =
                size - length;

        if (remaining > 0) {

            System.arraycopy(
                    buffer,
                    length,
                    buffer,
                    0,
                    remaining
            );
        }

        size = remaining;
    }

    public void flush() {

        while (size > 0) {
            emitLongest();
        }
    }

    public void clear() {
        size = 0;
    }

    public int size() {
        return size;
    }
}