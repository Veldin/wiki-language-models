package com.veldin.learners;

import com.veldin.SparseCounts;
import com.veldin.consumers.CodePointConsumer;
import com.veldin.finalmodels.FinalNGramModel;
import com.veldin.ngrampackstrategy.NGramPackStrategy;

import java.util.HashMap;
import java.util.Map;

public final class NGramLearner implements CodePointConsumer {

    private static final int VOCABULARY_SIZE = 255;

    // For pruning
    private static final int MAX_CONTEXTS = 25_000_000;
    private static final int MIN_OCCURRENCES = 2;

    private final Map<Integer, SparseCounts> counts =
            new HashMap<>();
    /*
     * Keep the last eight tokens in memory.
     */
    private int previous8 = 0;
    private int previous7 = 0;
    private int previous6 = 0;
    private int previous5 = 0;
    private int previous4 = 0;
    private int previous3 = 0;
    private int previous2 = 0;
    private int previous1 = 0;

    private final NGramPackStrategy packStrategy;

    public NGramLearner(NGramPackStrategy packStrategy) {
        this.packStrategy = packStrategy;
    }

    private long tokensProcessed = 0;

    @Override
    public void accept(int token) {

        if (token < 0 || token >= VOCABULARY_SIZE) {
            throw new IllegalArgumentException(
                    "Invalid vocabulary token: " + token
            );
        }

        packStrategy.addContexts(
                counts,
                previous8,
                previous7,
                previous6,
                previous5,
                previous4,
                previous3,
                previous2,
                previous1,
                token
        );

        previous8 = previous7;
        previous7 = previous6;
        previous6 = previous5;
        previous5 = previous4;
        previous4 = previous3;
        previous3 = previous2;
        previous2 = previous1;
        previous1 = token;

        if ((++tokensProcessed & 0xFFFFF) == 0) { // every ~1M tokens
            prune();
        }
    }

    public void prune() {


        if (counts.size() <= MAX_CONTEXTS) {
            return;
        }

        System.out.println(
                "Pruning "
                        + counts.size()
                        + " contexts; minimum occurrences = "
                        + MIN_OCCURRENCES
        );

        counts.entrySet().removeIf(
                entry -> {

                    SparseCounts possibilities =
                            entry.getValue();

                    int total = 0;

                    for (int i = 0;
                         i < possibilities.size();
                         i++) {

                        total += possibilities.getCount(i);
                    }

                    return total < MIN_OCCURRENCES;
                }
        );

        System.out.println(
                "After pruning: "
                        + counts.size()
                        + " contexts"
        );
    }

    public void merge(NGramLearner other) {

        if (other == this) {
            return;
        }

        for (var entry : other.counts.entrySet()) {

            SparseCounts target =
                    counts.computeIfAbsent(
                            entry.getKey(),
                            k -> new SparseCounts()
                    );

            SparseCounts source =
                    entry.getValue();

            for (int i = 0;
                 i < source.size();
                 i++) {

                int token =
                        source.getToken(i);

                int count =
                        source.getCount(i);

                target.add(token, count);
            }
        }
    }

    /*
     * Release all counts held by this learner.
     *
     * This is useful after merging a worker into another
     * learner, allowing the worker's large map to be
     * garbage-collected immediately.
     */
    public void clear() {

        counts.clear();

        previous8 = 0;
        previous7 = 0;
        previous6 = 0;
        previous5 = 0;
        previous4 = 0;
        previous3 = 0;
        previous2 = 0;
        previous1 = 0;
    }

    public FinalNGramModel toFinalNGramLearner(
            NGramPackStrategy strategy
    ) {

        /*
         * Keep the same ~75% load factor as FinalNGramBuilder.
         */
        int capacity = tableSizeFor(counts.size());

        int[] keys = new int[capacity];
        short[] values = new short[capacity];
        byte[] used = new byte[capacity];

        var iterator = counts.entrySet().iterator();

        while (iterator.hasNext()) {

            var entry = iterator.next();

            SparseCounts possibilities =
                    entry.getValue();

            int firstToken = -1;
            int firstCount = 0;

            int secondToken = -1;
            int secondCount = 0;

            /*
             * Only iterate over tokens that actually occurred
             * for this context.
             */
            for (int i = 0;
                 i < possibilities.size();
                 i++) {

                int token =
                        possibilities.getToken(i);

                int count =
                        possibilities.getCount(i);

                if (token == 0) {
                    continue;
                }

                if (count > firstCount) {

                    secondToken = firstToken;
                    secondCount = firstCount;

                    firstToken = token;
                    firstCount = count;

                } else if (count > secondCount) {

                    secondToken = token;
                    secondCount = count;
                }
            }

            if (firstToken != -1) {

                if (secondToken == -1) {
                    secondToken = 255;
                }

                int packed =
                        (secondToken << 8)
                                | firstToken;

                put(
                        keys,
                        values,
                        used,
                        entry.getKey(),
                        (short) packed
                );
            }

            /*
             * We no longer need this SparseCounts.
             */
            iterator.remove();
        }

        return new FinalNGramModel(
                keys,
                values,
                used,
                strategy
        );
    }

    private static void put(
            int[] keys,
            short[] values,
            byte[] used,
            int key,
            short value
    ) {
        int mask = keys.length - 1;
        int index = mix(key) & mask;

        while (used[index] != 0) {

            if (keys[index] == key) {
                values[index] = value;
                return;
            }

            index = (index + 1) & mask;
        }

        keys[index] = key;
        values[index] = value;
        used[index] = 1;
    }

    private static int mix(int value) {
        value ^= value >>> 16;
        value *= 0x7feb352d;
        value ^= value >>> 15;
        value *= 0x846ca68b;
        value ^= value >>> 16;
        return value;
    }

    private static int tableSizeFor(int entries) {

        if (entries < 1) {
            return 2;
        }

        /*
         * Keep load factor <= 75%.
         */
        long required =
                ((long) entries * 4L + 2L) / 3L;

        if (required > (1L << 30)) {
            throw new IllegalStateException(
                    "N-gram model is too large: "
                            + entries
                            + " entries"
            );
        }

        int capacity = 1;

        while (capacity < required) {
            capacity <<= 1;
        }

        return capacity;
    }
}