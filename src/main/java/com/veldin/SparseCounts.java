package com.veldin;

public final class SparseCounts {

    private static final int INITIAL_CAPACITY = 4;

    private byte[] tokens;
    private int[] counts;
    private int size;

    public SparseCounts() {
        this(INITIAL_CAPACITY);
    }

    public SparseCounts(int initialCapacity) {
        tokens = new byte[initialCapacity];
        counts = new int[initialCapacity];
    }

    public void increment(int token) {
        if (token < 0 || token > 255) {
            throw new IllegalArgumentException("Token must be 0-255");
        }

        int index = find(token);

        if (index >= 0) {
            counts[index]++;
            return;
        }

        ensureCapacity();

        tokens[size] = (byte) token;
        counts[size] = 1;
        size++;
    }

    public int get(int token) {
        int index = find(token);
        return index >= 0 ? counts[index] : 0;
    }

    public int getToken(int index) {
        return tokens[index] & 0xFF;
    }

    public int getCount(int index) {
        return counts[index];
    }

    public int size() {
        return size;
    }

    public boolean contains(int token) {
        return find(token) >= 0;
    }

    public void clear() {
        size = 0;
    }

    private int find(int token) {
        byte target = (byte) token;

        for (int i = 0; i < size; i++) {
            if (tokens[i] == target) {
                return i;
            }
        }

        return -1;
    }

    public void add(int token, int amount) {
        if (token < 0 || token > 255) {
            throw new IllegalArgumentException("Token must be 0-255");
        }

        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be >= 0");
        }

        int index = find(token);

        if (index >= 0) {
            counts[index] += amount;
            return;
        }

        ensureCapacity();

        tokens[size] = (byte) token;
        counts[size] = amount;
        size++;
    }

    private void ensureCapacity() {
        if (size < tokens.length) {
            return;
        }

        int newCapacity = tokens.length * 2;

        byte[] newTokens = new byte[newCapacity];
        int[] newCounts = new int[newCapacity];

        System.arraycopy(tokens, 0, newTokens, 0, size);
        System.arraycopy(counts, 0, newCounts, 0, size);

        tokens = newTokens;
        counts = newCounts;
    }


}