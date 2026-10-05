package com.veldin.exampleusage;

import com.veldin.finalmodels.FinalLanguageModels;
import com.veldin.finalmodels.FinalWordDictionary;
import com.veldin.tokens.SixBitWordToken;
import com.veldin.tokens.TokenSequence;

import java.util.ArrayList;
import java.util.List;

public final class DictionaryExamples {

    private DictionaryExamples() {
    }

    /**
     * Finds words in the input that are not present in the dictionary
     * and returns their nearest dictionary corrections.
     *
     * <p>The start/end positions use Java String indices and are
     * half-open: [start, end).</p>
     */
    public static List<WordResult> findUnknownWords(
            FinalLanguageModels model,
            String input,
            int maxDistance
    ) {
        if (model == null) {
            throw new IllegalArgumentException("Model cannot be null");
        }

        if (input == null) {
            throw new IllegalArgumentException("Input cannot be null");
        }

        if (maxDistance < 1) {
            throw new IllegalArgumentException(
                    "Maximum distance must be at least 1"
            );
        }

        List<WordResult> results = new ArrayList<>();

        int wordStart = -1;

        for (int i = 0; i < input.length(); i++) {
            char character = input.charAt(i);

            boolean wordCharacter =
                    Character.isLetter(character)
                            || character == '\''
                            || character == '-';

            if (wordCharacter) {
                if (wordStart == -1) {
                    wordStart = i;
                }
                continue;
            }

            if (wordStart != -1) {
                results.add(
                        checkWord(
                                model,
                                input,
                                wordStart,
                                i,
                                maxDistance
                        )
                );

                wordStart = -1;
            }
        }

        if (wordStart != -1) {
            results.add(
                    checkWord(
                            model,
                            input,
                            wordStart,
                            input.length(),
                            maxDistance
                    )
            );
        }

        return results;
    }

    private static WordResult checkWord(
            FinalLanguageModels model,
            String input,
            int start,
            int end,
            int maxDistance
    ) {
        String word = input.substring(start, end);
        TokenSequence tokens = FinalWordDictionary.stringToTokenSequence(word);

        if (model.dictionary.contains(tokens)) {
            return new WordResult(
                    word,
                    start,
                    end,
                    end - start,
                    true,
                    List.of()
            );
        }

        /*
         * The grouped lookup gives us:
         *
         * distance 0 -> exact matches
         * distance 1 -> one edit away
         * distance 2 -> two edits away
         * ...
         *
         * Find the first non-empty group. Those are the nearest
         * possible corrections.
         */
        List<List<String>> grouped =
                model.dictionary.getAllWithinDistanceGrouped(
                        word,
                        maxDistance
                );

        List<String> nearest = List.of();

        for (List<String> candidates : grouped) {
            if (!candidates.isEmpty()) {
                nearest = List.copyOf(candidates);
                break;
            }
        }

        return new WordResult(
                word,
                start,
                end,
                end - start,
                false,
                nearest
        );
    }

    public record WordResult(
            String word,
            int start,
            int end,
            int length,
            boolean recognised,
            List<String> corrections
    ) {
    }
}