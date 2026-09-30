package com.veldin.learners;

import com.veldin.consumers.CodePointConsumer;
import com.veldin.finalmodels.FinalWordDictionary;
import com.veldin.tokens.SixBitWordToken;
import com.veldin.tokens.TokenSequence;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public final class WordDictionaryLearner
        implements CodePointConsumer {

    private static final int INITIAL_WORD_CAPACITY = 16;

    /*
     * Word tokens -> occurrence count.
     */
    private final Map<TokenSequence, Integer> wordCounts =
            new HashMap<>();

    /*
     * Reusable current-word buffer.
     */
    private byte[] currentWord =
            new byte[INITIAL_WORD_CAPACITY];

    private int currentWordLength = 0;

    public WordDictionaryLearner() {
    }

    /**
     * Accept a 6-bit token.
     */
    @Override
    public void accept(int token) {

        if (token < 0
                || token > SixBitWordToken.TOKEN_MASK) {

            throw new IllegalArgumentException(
                    "Invalid token: " + token
            );
        }

        /*
         * NULL terminates the input.
         */
        if (token == SixBitWordToken.NULL_TOKEN) {
            finishWord();
            return;
        }

        /*
         * Unknown tokens are ignored.
         */
        if (token == SixBitWordToken.UNKNOWN_TOKEN) {
            return;
        }

        /*
         * Word separator.
         */
        if (token == SixBitWordToken.WORD_SEPARATOR_TOKEN) {
            finishWord();
            return;
        }

        /*
         * Sentence end also finishes the word.
         */
        if (token == SixBitWordToken.SENTENCE_END_TOKEN) {
            finishWord();
            return;
        }

        /*
         * Everything else is part of the current word.
         */
        append(token);
    }

    /**
     * Append a token to the current word.
     */
    private void append(int token) {

        ensureWordCapacity(
                currentWordLength + 1
        );

        currentWord[currentWordLength++] =
                (byte) token;
    }

    /**
     * Finish and store the current word.
     */
    private void finishWord() {

        if (currentWordLength == 0) {
            return;
        }

        TokenSequence word =
                new TokenSequence(
                        currentWord,
                        currentWordLength
                );

        if (isAcceptedWord(word)) {

            wordCounts.merge(
                    word,
                    1,
                    Integer::sum
            );
        }

        currentWordLength = 0;
    }

    /**
     * Determine whether a word should be stored.
     */
    private boolean isAcceptedWord(TokenSequence word) {

        boolean[] seen =
                new boolean[
                        SixBitWordToken.TOKEN_MASK + 1
                        ];

        int distinct = 0;

        int longestRun = 1;
        int currentRun = 1;

        for (int i = 0; i < word.size(); i++) {

            int token = word.get(i);

            if (!seen[token]) {
                seen[token] = true;
                distinct++;
            }

            if (i > 0) {

                if (token == word.get(i - 1)) {

                    currentRun++;

                    longestRun =
                            Math.max(
                                    longestRun,
                                    currentRun
                            );

                } else {
                    currentRun = 1;
                }
            }
        }

        /*
         * Four identical characters are always suspicious.
         */
        if (longestRun >= 4) {
            return false;
        }

        /*
         * Short words with three identical characters
         * are suspicious.
         */
        if (word.size() <= 5
                && longestRun >= 3) {

            return false;
        }

        /*
         * Longer words with very little character diversity
         * are suspicious when they contain a long repeated run.
         */
        if (word.size() > 5
                && longestRun >= 3
                && distinct <= 3) {

            return false;
        }

        return true;
    }

    /**
     * Finish the final word.
     */
    @Override
    public void finish() {
        finishWord();
    }

    /**
     * Grow the current word buffer.
     */
    private void ensureWordCapacity(int required) {

        if (required <= currentWord.length) {
            return;
        }

        int newCapacity =
                Math.max(
                        required,
                        currentWord.length * 2
                );

        currentWord =
                Arrays.copyOf(
                        currentWord,
                        newCapacity
                );
    }

    /**
     * Number of unique words.
     */
    public int size() {
        return wordCounts.size();
    }

    /**
     * Get all word counts.
     */
    public Map<TokenSequence, Integer> getWordCounts() {
        return wordCounts;
    }

    /**
     * Merge another word dictionary learner.
     */
    public void merge(WordDictionaryLearner other) {

        if (other == this) {
            return;
        }

        other.finish();

        for (var entry : other.wordCounts.entrySet()) {

            wordCounts.merge(
                    entry.getKey(),
                    entry.getValue(),
                    Integer::sum
            );
        }
    }

    /**
     * Clear the learner.
     */
    public void clear() {

        wordCounts.clear();

        currentWordLength = 0;
    }

    public FinalWordDictionary toFinalWordDictionary() {
        return new FinalWordDictionary(
                wordCounts.keySet()
        );
    }
}