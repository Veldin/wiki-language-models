package com.veldin.finalmodels;

import com.veldin.tokens.SixBitWordToken;
import com.veldin.tokens.TokenSequence;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class FinalWordDictionary {


    private static final int TOKEN_MASK = 0x3F;

    /*
     * Four 6-bit tokens occupy exactly three bytes.
     *
     *   6 + 6 + 6 + 6 = 24 bits = 3 bytes
     */
    private static final int TOKENS_PER_BLOCK = 4;
    private static final int BYTES_PER_BLOCK = 3;

    /*
     * offsets[i] points to the first packed byte of word i.
     *
     * offsets[wordCount] points one past the final byte.
     */
    private final int[] offsets;

    /*
     * Number of 6-bit tokens in every word.
     */
    public final int[] lengths;

    /*
     * All words packed consecutively.
     */
    public final byte[] data;

    /**
     * Creates an immutable final dictionary.
     * <p>
     * The supplied words are copied, sorted lexicographically,
     * and packed into the final representation.
     * <p>
     * Duplicate words are removed.
     */
    public FinalWordDictionary(Iterable<TokenSequence> words) {
        Objects.requireNonNull(words, "words");

        List<TokenSequence> sorted = new ArrayList<>();

        for (TokenSequence word : words) {
            Objects.requireNonNull(word, "word");

            if (word.size() == 0) {
                continue;
            }

            validateWord(word);
            sorted.add(word);
        }

        sorted.sort(FinalWordDictionary::compareWords);

        /*
         * Remove duplicates after sorting.
         */
        int uniqueCount = 0;

        for (TokenSequence word : sorted) {
            if (uniqueCount == 0
                    || compareWords(
                    sorted.get(uniqueCount - 1),
                    word
            ) != 0) {

                sorted.set(uniqueCount++, word);
            }
        }

        if (uniqueCount != sorted.size()) {
            sorted.subList(uniqueCount, sorted.size()).clear();
        }

        this.offsets = new int[sorted.size() + 1];
        this.lengths = new int[sorted.size()];

        long totalBytes = 0;

        for (int i = 0; i < sorted.size(); i++) {
            TokenSequence word = sorted.get(i);

            lengths[i] = word.size();

            totalBytes += packedLength(word.size());

            if (totalBytes > Integer.MAX_VALUE) {
                throw new IllegalArgumentException(
                        "Dictionary is too large for a Java byte[]: "
                                + totalBytes
                                + " bytes"
                );
            }
        }

        this.data = new byte[(int) totalBytes];

        int offset = 0;

        for (int i = 0; i < sorted.size(); i++) {
            offsets[i] = offset;

            TokenSequence word = sorted.get(i);

            int bytes = pack(
                    word,
                    data,
                    offset
            );

            offset += bytes;
        }

        offsets[sorted.size()] = offset;
    }

    public FinalWordDictionary(
            int[] offsets,
            int[] lengths,
            byte[] data
    ) {
        this.offsets = offsets;
        this.lengths = lengths;
        this.data = data;
    }

    /**
     * Number of words in the dictionary.
     */
    public int size() {
        return lengths.length;
    }

    /**
     * Returns the word at the specified sorted index.
     */
    public TokenSequence getWord(int index) {
        checkIndex(index);

        byte[] tokens = new byte[lengths[index]];

        unpack(
                data,
                offsets[index],
                lengths[index],
                tokens
        );

        return new TokenSequence(
                tokens,
                tokens.length
        );
    }

    /**
     * Returns true if the dictionary contains the supplied word.
     * <p>
     * Uses binary search over the sorted dictionary.
     */
    public boolean contains(TokenSequence word) {
        Objects.requireNonNull(word, "word");

        if (word.size() == 0) {
            return false;
        }

        validateWord(word);

        int low = 0;
        int high = size() - 1;

        while (low <= high) {
            int middle = (low + high) >>> 1;

            int comparison = compareWordToStored(
                    word,
                    middle
            );

            if (comparison < 0) {
                high = middle - 1;
            } else if (comparison > 0) {
                low = middle + 1;
            } else {
                return true;
            }
        }

        return false;
    }

    /**
     * Returns the index of the supplied word,
     * or -1 if it does not exist.
     */
    public int indexOf(TokenSequence word) {
        Objects.requireNonNull(word, "word");

        if (word.size() == 0) {
            return -1;
        }

        validateWord(word);

        int low = 0;
        int high = size() - 1;

        while (low <= high) {
            int middle = (low + high) >>> 1;

            int comparison = compareWordToStored(
                    word,
                    middle
            );

            if (comparison < 0) {
                high = middle - 1;
            } else if (comparison > 0) {
                low = middle + 1;
            } else {
                return middle;
            }
        }

        return -1;
    }


    /**
     * Helper – turns a TokenSequence back into a readable String.
     */
    public static String tokenSequenceToString(TokenSequence seq) {
        StringBuilder sb = new StringBuilder(seq.size());
        for (int i = 0; i < seq.size(); i++) {
            sb.append(Character.toChars(
                    SixBitWordToken.instance.toCodePoint(seq.get(i))));
        }
        return sb.toString();
    }

    /**
     * Converts a String into a TokenSequence.
     */
    public static TokenSequence stringToTokenSequence(String str) {
        Objects.requireNonNull(str, "str");

        byte[] tokens = new byte[
                str.codePointCount(0, str.length())
                ];

        int index = 0;

        for (int offset = 0; offset < str.length(); ) {
            int codePoint = str.codePointAt(offset);

            tokens[index++] = (byte)
                    SixBitWordToken.instance.toToken(codePoint);

            offset += Character.charCount(codePoint);
        }

        return new TokenSequence(tokens, tokens.length);
    }

    private int compareWordToStored(
            TokenSequence word,
            int storedIndex
    ) {
        int storedLength = lengths[storedIndex];
        int compareLength =
                Math.min(word.size(), storedLength);

        int offset = offsets[storedIndex];

        for (int i = 0; i < compareLength; i++) {
            int left = word.get(i);
            int right = getPackedToken(
                    data,
                    offset,
                    i
            );

            if (left != right) {
                return Integer.compare(left, right);
            }
        }

        return Integer.compare(
                word.size(),
                storedLength
        );
    }

    /**
     * Lexicographical comparison using the actual 6-bit token values.
     */
    private static int compareWords(
            TokenSequence a,
            TokenSequence b
    ) {
        int length =
                Math.min(a.size(), b.size());

        for (int i = 0; i < length; i++) {
            int left = a.get(i);
            int right = b.get(i);

            if (left != right) {
                return Integer.compare(left, right);
            }
        }

        return Integer.compare(
                a.size(),
                b.size()
        );
    }

    private static int pack(
            TokenSequence word,
            byte[] target,
            int offset
    ) {
        int length = word.size();
        int sourceIndex = 0;
        int targetIndex = offset;

        while (sourceIndex + 4 <= length) {
            int a = word.get(sourceIndex++);
            int b = word.get(sourceIndex++);
            int c = word.get(sourceIndex++);
            int d = word.get(sourceIndex++);

            target[targetIndex++] =
                    (byte) ((a << 2) | (b >>> 4));

            target[targetIndex++] =
                    (byte) ((b << 4) | (c >>> 2));

            target[targetIndex++] =
                    (byte) ((c << 6) | d);
        }

        int remaining = length - sourceIndex;

        if (remaining > 0) {
            int a = word.get(sourceIndex++);

            target[targetIndex++] =
                    (byte) (a << 2);

            if (remaining >= 2) {
                int b = word.get(sourceIndex++);

                target[targetIndex - 1] |=
                        (byte) (b >>> 4);

                target[targetIndex++] =
                        (byte) (b << 4);
            }

            if (remaining >= 3) {
                int c = word.get(sourceIndex);

                target[targetIndex - 1] |=
                        (byte) (c >>> 2);

                target[targetIndex++] =
                        (byte) (c << 6);
            }
        }

        return targetIndex - offset;
    }

    private static void unpack(
            byte[] source,
            int offset,
            int tokenCount,
            byte[] target
    ) {
        int sourceIndex = offset;
        int targetIndex = 0;

        while (targetIndex + 4 <= tokenCount) {
            int a = source[sourceIndex++] & 0xFF;
            int b = source[sourceIndex++] & 0xFF;
            int c = source[sourceIndex++] & 0xFF;

            target[targetIndex++] =
                    (byte) ((a >>> 2) & TOKEN_MASK);

            target[targetIndex++] =
                    (byte) (((a & 0x03) << 4)
                            | (b >>> 4));

            target[targetIndex++] =
                    (byte) (((b & 0x0F) << 2)
                            | (c >>> 6));

            target[targetIndex++] =
                    (byte) (c & TOKEN_MASK);
        }

        int remaining = tokenCount - targetIndex;

        if (remaining > 0) {
            int a = source[sourceIndex++] & 0xFF;

            target[targetIndex++] =
                    (byte) ((a >>> 2) & TOKEN_MASK);

            if (remaining >= 2) {
                int b = source[sourceIndex++] & 0xFF;

                target[targetIndex++] =
                        (byte) (((a & 0x03) << 4)
                                | (b >>> 4));

                if (remaining >= 3) {
                    target[targetIndex] =
                            (byte) (((b & 0x0F) << 2)
                                    | ((source[sourceIndex]
                                    & 0xFF) >>> 6));
                }
            }
        }
    }

    private static int getPackedToken(
            byte[] data,
            int offset,
            int tokenIndex
    ) {
        int block = tokenIndex >>> 2;
        int position = tokenIndex & 3;

        int byteOffset =
                offset + block * BYTES_PER_BLOCK;

        int a = data[byteOffset] & 0xFF;

        if (position == 0) {
            return (a >>> 2) & TOKEN_MASK;
        }

        int b = data[byteOffset + 1] & 0xFF;

        if (position == 1) {
            return ((a & 0x03) << 4)
                    | (b >>> 4);
        }

        int c = data[byteOffset + 2] & 0xFF;

        if (position == 2) {
            return ((b & 0x0F) << 2)
                    | (c >>> 6);
        }

        return c & TOKEN_MASK;
    }

    public static int packedLength(int tokenCount) {
        /*
         * ceil(tokenCount * 6 / 8)
         *
         * Written this way to avoid multiplication overflow.
         */
        return (tokenCount / TOKENS_PER_BLOCK)
                * BYTES_PER_BLOCK
                + switch (tokenCount % TOKENS_PER_BLOCK) {
            case 0 -> 0;
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            default -> throw new AssertionError();
        };
    }

    private static void validateWord(TokenSequence word) {
        for (int i = 0; i < word.size(); i++) {
            int token = word.get(i);

            if (token < 0 || token > SixBitWordToken.TOKEN_MASK) {
                throw new IllegalArgumentException(
                        "Invalid 6-bit token "
                                + token
                                + " at position "
                                + i
                );
            }
        }
    }

    public void validateOrdering() throws IOException {
        if (size() < 2) {
            return;
        }

        TokenSequence previous = getWord(0);

        for (int i = 1; i < size(); i++) {
            TokenSequence current = getWord(i);

            if (compareWords(previous, current) >= 0) {
                throw new IOException(
                        "Dictionary is not sorted at index "
                                + i
                );
            }

            previous = current;
        }
    }

    private void checkIndex(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException(
                    "index=" + index
                            + ", size=" + size()
            );
        }
    }

    public TokenSequence getClosest(
            TokenSequence word
    ) {
        Objects.requireNonNull(word, "word");

        if (word.size() == 0) {
            return null;
        }

        validateWord(word);

        int[] range = findPrefixRange(word);

        if (range == null) {
            return null;
        }

        int bestIndex = -1;
        int bestDistance = Integer.MAX_VALUE;

        for (int i = range[0]; i < range[1]; i++) {
            int distance =
                    levenshteinDistance(
                            word,
                            i,
                            bestDistance
                    );

            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;

                /*
                 * Exact match. We cannot do better than zero.
                 */
                if (bestDistance == 0) {
                    break;
                }
            }
        }

        return bestIndex < 0
                ? null
                : getWord(bestIndex);
    }

    public String getClosest(String word) {
        Objects.requireNonNull(word, "word");

        if (word.isEmpty()) {
            return null;
        }

        byte[] tokens =
                new byte[word.codePointCount(0, word.length())];

        int index = 0;

        for (int offset = 0; offset < word.length(); ) {
            int codePoint = word.codePointAt(offset);

            tokens[index++] =
                    (byte) SixBitWordToken.instance.toToken(codePoint);

            offset += Character.charCount(codePoint);
        }

        TokenSequence closest =
                getClosest(
                        new TokenSequence(
                                tokens,
                                tokens.length
                        )
                );

        if (closest == null) {
            return null;
        }

        StringBuilder result =
                new StringBuilder(closest.size());

        for (int i = 0; i < closest.size(); i++) {
            result.append(
                    Character.toChars(
                            SixBitWordToken.instance.toCodePoint(
                                    closest.get(i)
                            )
                    )
            );
        }

        return result.toString();
    }

    public List<TokenSequence> getAllWithinDistance(
            TokenSequence word,
            int maxDistance
    ) {
        Objects.requireNonNull(word, "word");

        if (maxDistance <= 0 || word.size() == 0) {
            return List.of();
        }

        validateWord(word);

        int[] range = findPrefixRange(word);

        if (range == null) {
            return List.of();
        }

        List<TokenSequence> result =
                new ArrayList<>();

        for (int i = range[0]; i < range[1]; i++) {
            int distance =
                    levenshteinDistance(
                            word,
                            i,
                            maxDistance
                    );

            if (distance < maxDistance) {
                result.add(getWord(i));
            }
        }

        return result;
    }

    public List<String> getAllWithinDistance(
            String word,
            int maxDistance
    ) {
        Objects.requireNonNull(word, "word");

        if (word.isEmpty() || maxDistance <= 0) {
            return List.of();
        }

        byte[] tokens =
                new byte[word.codePointCount(0, word.length())];

        int index = 0;

        for (int offset = 0; offset < word.length(); ) {
            int codePoint = word.codePointAt(offset);

            tokens[index++] =
                    (byte) SixBitWordToken.instance.toToken(codePoint);

            offset += Character.charCount(codePoint);
        }

        List<TokenSequence> closest =
                getAllWithinDistance(
                        new TokenSequence(
                                tokens,
                                tokens.length
                        ),
                        maxDistance
                );

        if (closest.isEmpty()) {
            return List.of();
        }

        List<String> result =
                new ArrayList<>(closest.size());

        for (TokenSequence sequence : closest) {
            StringBuilder builder =
                    new StringBuilder(sequence.size());

            for (int i = 0; i < sequence.size(); i++) {
                builder.append(
                        Character.toChars(
                                SixBitWordToken.instance.toCodePoint(
                                        sequence.get(i)
                                )
                        )
                );
            }

            result.add(builder.toString());
        }

        return result;
    }

    public List<List<String>> getAllWithinDistanceGrouped(
            TokenSequence word,
            int maxDistance
    ) {
        Objects.requireNonNull(word, "word");

        if (maxDistance < 0 || word.size() == 0) {
            return List.of();
        }

        validateWord(word);

        // One list for every possible distance: 0..maxDistance.
        List<List<String>> result = new ArrayList<>(maxDistance + 1);

        for (int i = 0; i <= maxDistance; i++) {
            result.add(new ArrayList<>());
        }

        // Check every dictionary entry to avoid missing corrections
        // that differ in their first or second token.
        for (int i = 0; i < size(); i++) {

            // Skip candidates whose length difference is too large.
            if (Math.abs(word.size() - lengths[i]) > maxDistance) {
                continue;
            }

            int distance = levenshteinDistance(
                    word,
                    i,
                    maxDistance
            );

            if (distance <= maxDistance) {
                result.get(distance).add(
                        tokenSequenceToString(getWord(i))
                );
            }
        }

        return result;
    }

    public List<List<String>> getAllWithinDistanceGrouped(
            String word,
            int maxDistance
    ) {
        Objects.requireNonNull(word, "word");

        if (word.isEmpty() || maxDistance < 0) {
            return List.of();
        }

        byte[] tokens = new byte[
                word.codePointCount(0, word.length())
                ];

        int index = 0;

        for (int offset = 0; offset < word.length(); ) {
            int codePoint = word.codePointAt(offset);

            tokens[index++] = (byte)
                    SixBitWordToken.instance.toToken(codePoint);

            offset += Character.charCount(codePoint);
        }

        return getAllWithinDistanceGrouped(
                new TokenSequence(tokens, tokens.length),
                maxDistance
        );
    }

    @Override
    public String toString() {
        return "FinalWordDictionary{"
                + "size=" + size()
                + ", data=" + data.length
                + " bytes"
                + '}';
    }

    /**
     * Finds the [start, end) range of words that have the
     * same first two tokens as the supplied word.
     * <p>
     * Because the dictionary is sorted, all words with the
     * same prefix are adjacent.
     */
    private int[] findPrefixRange(
            TokenSequence word
    ) {
        /*
         * With fewer than two tokens there is no useful
         * two-token prefix to search for.
         *
         * In that case, simply consider the entire dictionary.
         */
        if (word.size() < 2) {
            if (size() == 0) {
                return null;
            }

            return new int[]{
                    0,
                    size()
            };
        }

        int first = word.get(0);
        int second = word.get(1);

        int low = 0;
        int high = size();

        /*
         * Find first word >= prefix.
         */
        while (low < high) {
            int middle = (low + high) >>> 1;

            if (compareStoredPrefix(
                    middle,
                    first,
                    second
            ) < 0) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }

        int start = low;

        /*
         * Nothing starts with this prefix.
         */
        if (start >= size()
                || !hasPrefix(
                start,
                first,
                second
        )) {
            return null;
        }

        /*
         * Find first word > prefix.
         *
         * Since words beginning with the prefix are adjacent,
         * we can search using the same prefix comparison.
         */
        low = start;
        high = size();

        while (low < high) {
            int middle = (low + high) >>> 1;

            if (hasPrefix(
                    middle,
                    first,
                    second
            )) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }

        return new int[]{
                start,
                low
        };
    }

    /**
     * Compares the first two tokens of a stored word
     * with the requested prefix.
     */
    private int compareStoredPrefix(
            int index,
            int first,
            int second
    ) {
        int len = lengths[index];
        int offset = offsets[index];

        if (len == 0) {
            return -1;
        }

        int storedFirst = getPackedToken(data, offset, 0);
        int cmp = Integer.compare(storedFirst, first);
        if (cmp != 0) {
            return cmp;
        }

        // first token equal
        if (len == 1) {
            // shorter word comes before any longer word with the same first token
            return -1;
        }

        int storedSecond = getPackedToken(data, offset, 1);
        return Integer.compare(storedSecond, second);
    }


    /**
     * Returns whether a stored word starts with
     * the specified two-token prefix.
     */
    private boolean hasPrefix(
            int index,
            int first,
            int second
    ) {
        if (lengths[index] < 2) {
            return false;
        }

        int offset = offsets[index];

        return getPackedToken(
                data,
                offset,
                0
        ) == first
                && getPackedToken(
                data,
                offset,
                1
        ) == second;
    }

    /**
     * Calculates the Levenshtein distance between the supplied
     * word and a packed dictionary word.
     * <p>
     * The calculation stops early when the distance cannot
     * improve upon maxDistance.
     */
    private int levenshteinDistance(
            TokenSequence word,
            int storedIndex,
            int maxDistance
    ) {
        int wordLength =
                word.size();

        int candidateLength =
                lengths[storedIndex];

        /*
         * If the length difference is already greater than
         * the best distance we have, this candidate cannot win.
         */
        if (Math.abs(
                wordLength - candidateLength
        ) > maxDistance) {
            return maxDistance + 1;
        }

        /*
         * We only need two rows of the Levenshtein matrix.
         */
        int[] previous =
                new int[candidateLength + 1];

        int[] current =
                new int[candidateLength + 1];

        for (int j = 0;
             j <= candidateLength;
             j++) {

            previous[j] = j;
        }

        int offset =
                offsets[storedIndex];

        for (int i = 1;
             i <= wordLength;
             i++) {

            current[0] = i;

            int minimumInRow =
                    current[0];

            int sourceToken =
                    word.get(i - 1);

            for (int j = 1;
                 j <= candidateLength;
                 j++) {

                int candidateToken =
                        getPackedToken(
                                data,
                                offset,
                                j - 1
                        );

                int insertion =
                        current[j - 1] + 1;

                int deletion =
                        previous[j] + 1;

                int substitution =
                        previous[j - 1]
                                + (
                                sourceToken
                                        == candidateToken
                                        ? 0
                                        : 1
                        );

                int value =
                        Math.min(
                                Math.min(
                                        insertion,
                                        deletion
                                ),
                                substitution
                        );

                current[j] = value;

                minimumInRow =
                        Math.min(
                                minimumInRow,
                                value
                        );
            }

            /*
             * If the entire row is already beyond the
             * allowed distance, there is no point continuing.
             */
            if (minimumInRow > maxDistance) {
                return maxDistance + 1;
            }

            int[] swap = previous;
            previous = current;
            current = swap;
        }

        return previous[candidateLength];
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }

        if (!(obj instanceof FinalWordDictionary other)) {
            return false;
        }

        return java.util.Arrays.equals(offsets, other.offsets)
                && java.util.Arrays.equals(lengths, other.lengths)
                && java.util.Arrays.equals(data, other.data);
    }
}