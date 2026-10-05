package com.veldin.exampleusage;

import com.veldin.VocabularyTokenizer;
import com.veldin.finalmodels.FinalLanguageModels;
import com.veldin.tokens.SixBitWordToken;

import java.util.ArrayList;
import java.util.List;

public final class PredictionExamples {

    private PredictionExamples() {
    }

    public static List<String> predictBranches(
            FinalLanguageModels model,
            String input,
            int maxBranches,
            int maxCharacters
    ) {
        if (maxBranches < 1) {
            throw new IllegalArgumentException(
                    "Max branches must be at least 1"
            );
        }

        if (maxCharacters < 1) {
            throw new IllegalArgumentException(
                    "Max characters must be at least 1"
            );
        }

        int[] context =
                tokenizeContext(
                        model,
                        input
                );

        List<Branch> branches =
                new ArrayList<>();

        branches.add(
                new Branch(
                        context,
                        ""
                )
        );

        List<String> finished =
                new ArrayList<>();

        while (!branches.isEmpty()
                && finished.size() < maxBranches) {

            List<Branch> nextBranches =
                    new ArrayList<>();

            for (Branch branch : branches) {

                List<Integer> predictions =
                        predictAllWithBackoff(
                                model,
                                branch.context
                        );

                if (predictions.isEmpty()) {
                    if (!branch.text.isEmpty()) {
                        finished.add(branch.text);
                    }
                    continue;
                }

                for (int nextToken : predictions) {

                    if (nextToken ==
                            SixBitWordToken.NULL_TOKEN) {
                        continue;
                    }

                    String tokenText =
                            model.vocabulary.getText(
                                    nextToken
                            );

                    if (tokenText == null) {
                        continue;
                    }

                    String text =
                            branch.text + tokenText;

                    if (text.length() >= maxCharacters) {
                        finished.add(
                                text.substring(
                                        0,
                                        maxCharacters
                                )
                        );

                        if (finished.size()
                                >= maxBranches) {
                            break;
                        }

                        continue;
                    }

                    nextBranches.add(
                            new Branch(
                                    shift(
                                            branch.context,
                                            nextToken
                                    ),
                                    text
                            )
                    );
                }

                if (finished.size()
                        >= maxBranches) {
                    break;
                }
            }

            branches = nextBranches;
        }

        return finished;
    }

    private static int[] tokenizeContext(
            FinalLanguageModels model,
            String input
    ) {
        VocabularyTokenizer tokenizer =
                new VocabularyTokenizer(
                        model.vocabulary
                );

        tokenizer.accept(
                new StringBuilder(input)
        );
        tokenizer.finish();

        var tokens =
                tokenizer.getTokens();

        int[] context =
                new int[8];

        for (int token : tokens) {
            System.arraycopy(
                    context,
                    1,
                    context,
                    0,
                    7
            );

            context[7] = token;
        }

        return context;
    }

    private static List<Integer> predictAllWithBackoff(
            FinalLanguageModels model,
            int[] originalContext
    ) {
        int[] context =
                originalContext.clone();

        for (int start = 0;
             start < context.length;
             start++) {

            List<Integer> predictions =
                    model.learner.predictAll(
                            context[0],
                            context[1],
                            context[2],
                            context[3],
                            context[4],
                            context[5],
                            context[6],
                            context[7]
                    );

            if (!predictions.isEmpty()) {
                return predictions;
            }

            context[start] = 0;
        }

        return List.of();
    }

    private static int[] shift(
            int[] context,
            int nextToken
    ) {
        return new int[]{
                context[1],
                context[2],
                context[3],
                context[4],
                context[5],
                context[6],
                context[7],
                nextToken
        };
    }

    private record Branch(
            int[] context,
            String text
    ) {
    }
}