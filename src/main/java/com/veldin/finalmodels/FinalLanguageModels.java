package com.veldin.finalmodels;

import com.veldin.VocabularyTokenizer;
import com.veldin.tokens.SixBitWordToken;

import java.util.List;

public final class FinalLanguageModels {

    public final FinalWordDictionary dictionary;
    public final FinalVocabulary vocabulary;
    public final FinalNGramModel learner;

    public FinalLanguageModels(
            FinalWordDictionary dictionary,
            FinalVocabulary vocabulary,
            FinalNGramModel learner
    ) {
        this.dictionary = dictionary;
        this.vocabulary = vocabulary;
        this.learner = learner;
    }

    /*
     * ============================================================
     * STRING
     * ============================================================
     */

    public void generate(
            StringBuilder text,
            int tokenCount
    ) {

        if (tokenCount < 0) {
            throw new IllegalArgumentException(
                    "Token count cannot be negative"
            );
        }

        VocabularyTokenizer tokenizer =
                new VocabularyTokenizer(
                        vocabulary
                );

        tokenizer.accept(text);
        tokenizer.finish();

        var tokens = tokenizer.getTokens();

        int p8 = 0;
        int p7 = 0;
        int p6 = 0;
        int p5 = 0;
        int p4 = 0;
        int p3 = 0;
        int p2 = 0;
        int p1 = 0;

        for (int token : tokens) {

            p8 = p7;
            p7 = p6;
            p6 = p5;
            p5 = p4;
            p4 = p3;
            p3 = p2;
            p2 = p1;
            p1 = token;
        }

        System.out.println(
                "Context window ["
                        + vocabulary.getText(p8)
                        + vocabulary.getText(p7)
                        + vocabulary.getText(p6)
                        + vocabulary.getText(p5)
                        + vocabulary.getText(p4)
                        + vocabulary.getText(p3)
                        + vocabulary.getText(p2)
                        + vocabulary.getText(p1)
                        + "]"
        );

        /*
         * Generate the requested number of tokens.
         */
        for (int i = 0;
             i < tokenCount;
             i++) {

            int nextToken = predictWithBackoff(
                    p8,
                    p7,
                    p6,
                    p5,
                    p4,
                    p3,
                    p2,
                    p1
            );

            /*
             * No prediction available.
             */
            if (nextToken == -1) {
                System.out.println(
                        "No prediction available."
                );
                break;
            }

            /*
             * NULL cannot be converted back
             * into meaningful vocabulary text.
             */
            if (nextToken ==
                    SixBitWordToken.NULL_TOKEN) {

                break;
            }

            String generated =
                    vocabulary.getText(
                            nextToken
                    );

            if (generated == null) {
                break;
            }

            if (i == 0) {
                System.out.println(
                        "First next token [" + nextToken + "] '" +
                                generated + "'"
                );
            }

            text.append(generated);

            /*
             * Shift history.
             */
            p8 = p7;
            p7 = p6;
            p6 = p5;
            p5 = p4;
            p4 = p3;
            p3 = p2;
            p2 = p1;
            p1 = nextToken;
        }
    }

    /*
     * ============================================================
     * PREDICTION WITH BACKOFF
     * ============================================================
     *
     * Try:
     *
     * p8 p7 p6 p5 p4 p3 p2 p1
     *  0 p7 p6 p5 p4 p3 p2 p1
     *  0  0 p6 p5 p4 p3 p2 p1
     *  ...
     *  0  0  0  0  0  0  0 p1
     *
     * The oldest context token is removed first.
     */
    private int predictWithBackoff(
            int p8,
            int p7,
            int p6,
            int p5,
            int p4,
            int p3,
            int p2,
            int p1
    ) {

        int[] context = {
                p8, p7, p6, p5,
                p4, p3, p2, p1
        };

        for (int start = 0;
             start < context.length;
             start++) {

            int nextToken =
                    learner.predict(
                            context[0],
                            context[1],
                            context[2],
                            context[3],
                            context[4],
                            context[5],
                            context[6],
                            context[7]
                    );

            if (nextToken != -1) {
                return nextToken;
            }

            /*
             * Remove the oldest remaining token.
             *
             * Example:
             *
             * [p8,p7,p6,p5,p4,p3,p2,p1]
             * [ 0,p7,p6,p5,p4,p3,p2,p1]
             * [ 0, 0,p6,p5,p4,p3,p2,p1]
             */
            context[start] = 0;
        }

        return -1;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                dictionary,
                vocabulary,
                learner
        );
    }

}