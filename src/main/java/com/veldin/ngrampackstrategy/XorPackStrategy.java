package com.veldin.ngrampackstrategy;

import com.veldin.SparseCounts;

import java.util.Map;

public final class XorPackStrategy implements NGramPackStrategy {

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
        int context1;
        int context2;
        int context3;
        int context4;

        if (previous2 == 0) { // P One is available
            // 1-gram: p1
            context1 = 0;
            context2 = 0;
            context3 = 0;
            context4 = previous1;

        } else if (previous3 == 0) { // P Two is available
            // 2-gram: p2, p1
            context1 = 0;
            context2 = 0;
            context3 = previous2;
            context4 = previous1;

        } else if (previous4 == 0) { // P Three is available
            // 2-gram: p2, p1
            context1 = 0;
            context2 = rotateXor(previous3, 0);
            context3 = previous2;
            context4 = previous1;

        } else if (previous5 == 0) { // P Four is available

            context1 = 0;
            context2 = rotateXor(previous3, previous4);
            context3 = previous2;
            context4 = previous1;

        } else if (previous7 == 0) { // P Six is available

            context1 = rotateXor(previous6, 0, 0);
            context2 = rotateXor(previous3, previous4);
            context3 = previous2;
            context4 = previous1;

        } else if (previous8 == 0) { // P Seven is available

            context1 = rotateXor(previous6, previous7, 0);
            context2 = rotateXor(previous3, previous4);
            context3 = previous2;
            context4 = previous1;

        } else { // P Eight is available
            // 5-gram:
            // mix(p8, p7, p6), p5, p4, p3
            context1 = rotateXor(previous6, previous7, previous8);
            context2 = rotateXor(previous3, previous4);
            context3 = previous2;
            context4 = previous1;
        }

        return NGramPackStrategy.pack4(
                context1,
                context2,
                context3,
                context4
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
            int token
    ) {
        // P1
        add(counts,
                0, 0, 0, 0,
                0, 0, 0, previous1,
                token);

        // P2
        add(counts,
                0, 0, 0, 0,
                0, 0, previous2, previous1,
                token);

        // P3
        add(counts,
                0, 0, 0, 0,
                0, previous3, previous2, previous1,
                token);

        // P4
        add(counts,
                0, 0, 0, 0,
                previous4, previous3, previous2, previous1,
                token);

        // P6
        add(counts,
                0, 0, previous6, 0,
                previous4, previous3, previous2, previous1,
                token);

        // P7
        add(counts,
                0, previous7, previous6, 0,
                previous4, previous3, previous2, previous1,
                token);

        // P8
        add(counts,
                previous8, previous7, previous6, previous5,
                previous4, previous3, previous2, previous1,
                token);
    }

    private static int rotateXor(int... tokens) {
        int result = 0;

        for (int token : tokens) {
            result = Integer.rotateLeft(result, 5) ^ token;
        }

        return result & 0xFF;
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