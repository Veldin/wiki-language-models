package com.veldin.ngrampackstrategy;

import com.veldin.SparseCounts;

import java.util.Map;

public final class SkippingPackStrategy implements NGramPackStrategy {

    @Override
    public int pack(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1
    ) {
        return NGramPackStrategy.pack4(
                previous7,
                previous4,
                previous2,
                previous1
        );
    }

    @Override
    public void addContexts(
            Map<Integer, SparseCounts> counts,
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1,
            // the token we are currently processing.
            int token
    ) {
        add(counts,
                0, previous7, 0, 0,
                previous4, 0, previous2, previous1,
                token);

        add(counts,
                0, 0, 0, 0,
                previous4, 0, previous2, previous1,
                token);

        add(counts,
                0, 0, 0, 0,
                0, 0, previous2, previous1,
                token);

        add(counts,
                0, 0, 0, 0,
                0, 0, 0, previous1,
                token);
    }

    private void add(
            Map<Integer, SparseCounts> counts,
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1,
            int token
    ) {
        int key = pack(
                previous8,
                previous7,
                previous6,
                previous5,
                previous4,
                previous3,
                previous2,
                previous1
        );

        SparseCounts sparseCounts =
                counts.computeIfAbsent(
                        key,
                        k -> new SparseCounts()
                );

        sparseCounts.increment(token);
    }
}