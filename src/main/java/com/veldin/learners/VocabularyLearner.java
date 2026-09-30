package com.veldin.learners;

import com.veldin.LongLongHashMap;
import com.veldin.consumers.CodePointConsumer;
import com.veldin.tokens.SixBitWordToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class VocabularyLearner implements CodePointConsumer {

    private static final int MAX_SEQUENCE_LENGTH = 9;

    private static final long[] SEQUENCE_MASKS = new long[MAX_SEQUENCE_LENGTH + 1];

    static {
        for (int length = 1; length <= MAX_SEQUENCE_LENGTH; length++) {

            SEQUENCE_MASKS[length] = (1L << (SixBitWordToken.TOKEN_BITS * length)) - 1;
        }
    }

    /*
     * Maximum number of candidates retained for each
     * sequence length.
     *
     * This is the important memory bound.
     */
    private final int maxEntriesPerLength;

    /*
     * We allow the map to grow somewhat beyond the limit
     * before pruning.
     *
     * Example:
     *
     * maxEntries = 10_000
     * prune threshold = 20_000
     *
     * The map is then reduced back to 10_000.
     */
    private final int pruneThreshold;

    private final LongLongHashMap[] frequencies;

    private long sequence = 0;
    private int sequenceLength = 0;

    public VocabularyLearner() {
        this(3_000_000);
    }

    public VocabularyLearner(int maxEntriesPerLength) {

        if (maxEntriesPerLength < 100) {
            throw new IllegalArgumentException("maxEntriesPerLength must be >= 100");
        }

        this.maxEntriesPerLength = maxEntriesPerLength;

        this.pruneThreshold = maxEntriesPerLength * 2;

        frequencies = new LongLongHashMap[MAX_SEQUENCE_LENGTH + 1];

        for (int i = 1; i <= MAX_SEQUENCE_LENGTH; i++) {
            frequencies[i] = new LongLongHashMap(16);
        }
    }

    /*
     * ============================================================
     * INPUT
     * ============================================================
     */
    @Override
    public void accept(int token) {

        if (token == SixBitWordToken.UNKNOWN_TOKEN) {
            return;
        }

        sequence = (sequence << SixBitWordToken.TOKEN_BITS) | token;

        sequenceLength =
                Math.min(
                        sequenceLength + 1,
                        MAX_SEQUENCE_LENGTH
                );

        for (int length = 1; length <= sequenceLength; length++) {

            LongLongHashMap map = frequencies[length];

            long key = sequence & SEQUENCE_MASKS[length];

            map.addTo(key, 1L);

            if (map.size() >= pruneThreshold) {
                prune(map);
            }
        }

        if (sequenceLength == MAX_SEQUENCE_LENGTH) {
            sequence &=
                    SEQUENCE_MASKS[MAX_SEQUENCE_LENGTH];
        }
    }

    /*
     * ============================================================
     * PRUNING
     * ============================================================
     */

    private void prune(LongLongHashMap map) {
        if (map.size() > maxEntriesPerLength) {
            map.retainTopK(maxEntriesPerLength);
        }
    }

    /*
     * ============================================================
     * FINAL PRUNE
     * ============================================================
     */

    public void finish() {

        for (int length = 1; length <= MAX_SEQUENCE_LENGTH; length++) {

            prune(frequencies[length]);
        }
    }

    /*
     * ============================================================
     * GETTERS
     * ============================================================
     */

    public LongLongHashMap getOneCharFrequency() {
        return frequencies[1];
    }

    public LongLongHashMap getTwoCharFrequency() {
        return frequencies[2];
    }

    public LongLongHashMap getThreeCharFrequency() {
        return frequencies[3];
    }

    public LongLongHashMap getFourCharFrequency() {
        return frequencies[4];
    }

    public LongLongHashMap getFiveCharFrequency() {
        return frequencies[5];
    }

    public LongLongHashMap getSixCharFrequency() {
        return frequencies[6];
    }

    public LongLongHashMap getSevenCharFrequency() {
        return frequencies[7];
    }

    public LongLongHashMap getEightCharFrequency() {
        return frequencies[8];
    }

    public LongLongHashMap getNineCharFrequency() {
        return frequencies[9];
    }

    /*
     * ============================================================
     * WEIGHTING
     * ============================================================
     */

    private static final double CHAR_LENGTH_WEIGHT = 0.1;

    public Map<Long, Long> weightedCharFrequency() {

        finish();

        Map<Long, Long> weighted = new HashMap<>();

        for (int length = 1; length <= MAX_SEQUENCE_LENGTH; length++) {

            addWeighted(weighted, frequencies[length], length);
        }

        return weighted;
    }

    private void addWeighted(
            Map<Long, Long> target,
            LongLongHashMap source,
            int length) {

        double lengthWeight = Math.pow(length, CHAR_LENGTH_WEIGHT);

        source.forEach((key, frequency) -> {
            double frequencyWeight = Math.log1p(frequency);
            long weighted = Math.round(frequencyWeight * lengthWeight);

            target.merge(key, weighted, Long::sum);
        });
    }

    /*
     * ============================================================
     * BUILD VOCABULARY
     * ============================================================
     */
    public long[] buildVocabulary() {

        finish();

        Map<Long, Long> weighted = weightedCharFrequency();

        List<Map.Entry<Long, Long>> entries =
                new ArrayList<>(weighted.entrySet());

        entries.sort(
                Map.Entry.<Long, Long>comparingByValue().reversed()
        );

        long[] result = new long[255];

        List<Map.Entry<Long, Long>> selected =
                new ArrayList<>(254);

        // Fill the vocab with all possible single characters first.
        for (int token = SixBitWordToken.FIRST_CHARACTER_TOKEN;
             token <= SixBitWordToken.LAST_CHARACTER_TOKEN;
             token++) {

            long key = token;

            selected.add(
                    Map.entry(
                            key,
                            frequencies[1].getOrDefault(key, 0L)
                    )
            );
        }

        // Add sentence end manually.
        long sentenceEnd =
                SixBitWordToken.SENTENCE_END_TOKEN;

        selected.add(
                Map.entry(
                        sentenceEnd,
                        frequencies[1].getOrDefault(sentenceEnd, 0L)
                )
        );

        for (Map.Entry<Long, Long> entry : entries) {

            if (selected.size() >= 254) {
                break;
            }

            String candidate = decode(entry.getKey());

            // Single-character sequences are already included above.
            if (candidate.length() == 1) {
                continue;
            }

            boolean contained = false;

            for (Map.Entry<Long, Long> existing : selected) {

                String existingString = decode(existing.getKey());

                if (existingString.length() <= candidate.length()) {
                    continue;
                }

                if (existingString.contains(candidate)) {
                    contained = true;
                    break;
                }
            }

            if (!contained) {
                selected.add(entry);
            }
        }

        for (int i = 0; i < selected.size(); i++) {
            result[i + 1] = selected.get(i).getKey();
        }

        return result;
    }

    /*
     * ============================================================
     * MERGE
     * ============================================================
     */

    public void merge(VocabularyLearner other) {

        other.finish();

        for (int length = 1; length <= MAX_SEQUENCE_LENGTH; length++) {

            mergeMap(frequencies[length], other.frequencies[length]);
        }

        /*
         * Make sure merging cannot cause unbounded growth.
         */
        finish();
    }

    private void mergeMap(LongLongHashMap target, LongLongHashMap source) {

        source.forEach((key, value) -> {
            target.addTo(key, value);

            if (target.size() >= pruneThreshold) {
                prune(target);
            }
        });

        prune(target);
    }

    /*
     * ============================================================
     * DECODING
     * ============================================================
     */

    public String decode(long sequence) {

        if (sequence == 0) {
            return "";
        }

        int bits = 64 - Long.numberOfLeadingZeros(sequence);

        int length = (bits + SixBitWordToken.TOKEN_BITS - 1) / SixBitWordToken.TOKEN_BITS;

        return decode(sequence, length);
    }

    public String decode(long sequence, int length) {

        if (length < 1 || length > MAX_SEQUENCE_LENGTH) {
            throw new IllegalArgumentException(
                    "Length must be between 1 and " + MAX_SEQUENCE_LENGTH
            );
        }

        StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {

            int token = (int) (sequence & SixBitWordToken.TOKEN_MASK);

            if (token == SixBitWordToken.NULL_TOKEN) {
                break;
            }

            result.appendCodePoint(
                    SixBitWordToken.instance.toCodePoint(token)
            );

            sequence >>= SixBitWordToken.TOKEN_BITS;
        }

        return result.reverse().toString();
    }
}