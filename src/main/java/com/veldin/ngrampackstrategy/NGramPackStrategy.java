package com.veldin.ngrampackstrategy;

import com.veldin.SparseCounts;

import java.util.Map;

public interface NGramPackStrategy {

    /*
     * Explains how does this strategy represent an 8-token history as an int
     */
    int pack(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1);

    /**
     * Builds a context from the previous 8 tokens.
     * <p>
     * Each token must be in the range 0..255.
     * The returned long contains the 4 tokens packed.
     */
    void addContexts(
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
    );

    static int pack4(int a, int b, int c, int d) {
        return (a << 24)
                | (b << 16)
                | (c << 8)
                | d;
    }
}