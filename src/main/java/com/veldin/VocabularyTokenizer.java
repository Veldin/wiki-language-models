package com.veldin;

import com.veldin.consumers.CodePointConsumer;
import com.veldin.finalmodels.FinalVocabulary;
import com.veldin.tokens.SixBitWordToken;

import java.util.ArrayList;
import java.util.List;

public final class VocabularyTokenizer
        implements CodePointConsumer {

    private static final int MAX_SEQUENCE_LENGTH = 9;

    private final FinalVocabulary vocabulary;

    private final StringBuilder buffer =
            new StringBuilder();

    private final List<Integer> tokens =
            new ArrayList<>();

    public VocabularyTokenizer(
            FinalVocabulary vocabulary
    ) {
        this.vocabulary = vocabulary;
    }

    @Override
    public void accept(int codePoint) {

        buffer.appendCodePoint(codePoint);
        process();
    }

    public void accept(String text) {

        for (int i = 0;
             i < text.length(); ) {

            int codePoint =
                    text.codePointAt(i);

            accept(codePoint);

            i += Character.charCount(codePoint);
        }
    }

    public void accept(StringBuilder text) {
        accept(text.toString());
    }

    public void finish() {

        while (!buffer.isEmpty()) {
            processOne();
        }
    }

    public List<Integer> getTokens() {
        return List.copyOf(tokens);
    }

    private void process() {

        while (buffer.length() >= MAX_SEQUENCE_LENGTH) {
            processOne();
        }
    }

    private void processOne() {

        if (buffer.isEmpty()) {
            return;
        }

        int bestToken =
                FinalVocabulary.UNKNOWN_TOKEN;

        int bestLength = 0;

        int maxLength =
                Math.min(
                        MAX_SEQUENCE_LENGTH,
                        buffer.length()
                );

        /*
         * Longest vocabulary sequence wins.
         */
        for (int length = maxLength;
             length >= 1;
             length--) {

            long sequence =
                    encode(
                            buffer.substring(
                                    0,
                                    length
                            )
                    );

            int token =
                    vocabulary.getToken(
                            sequence
                    );

            if (token !=
                    FinalVocabulary.UNKNOWN_TOKEN) {

                bestToken = token;
                bestLength = length;

                break;
            }
        }

        /*
         * No vocabulary sequence matched.
         */
        if (bestToken ==
                FinalVocabulary.UNKNOWN_TOKEN) {

            buffer.deleteCharAt(0);
            return;
        }

        tokens.add(bestToken);

        buffer.delete(
                0,
                bestLength
        );
    }

    private long encode(String text) {

        long result = 0;

        for (int i = 0;
             i < text.length(); ) {

            int codePoint =
                    text.codePointAt(i);

            int token =
                    SixBitWordToken.instance.toToken(
                            codePoint
                    );

            result =
                    (result << SixBitWordToken.TOKEN_BITS)
                            | token;

            i += Character.charCount(codePoint);
        }

        return result;
    }
}