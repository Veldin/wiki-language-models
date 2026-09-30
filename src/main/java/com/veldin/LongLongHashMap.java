package com.veldin;

import java.util.Arrays;
import java.util.Objects;

/**
 * A primitive {@code long -> long} hash map specialized for counting encoded
 * sequences.
 *
 * <p>All {@code long} values are valid keys, including zero. Occupancy is
 * tracked separately from the key and value arrays. Values may be zero or
 * negative, although frequency-counting callers will normally use positive
 * values.</p>
 *
 * <p>The map uses open addressing with linear probing. It does not allocate
 * wrapper objects for keys, values, or entries.</p>
 */
public final class LongLongHashMap {

    private static final float LOAD_FACTOR = 0.70f;
    private static final int MIN_CAPACITY = 2;
    private static final int MAX_CAPACITY = 1 << 30;

    private long[] keys;
    private long[] values;
    private boolean[] occupied;
    private int mask;
    private int maxFill;
    private int size;

    /**
     * Creates a map with room for approximately 11 entries before its first
     * resize.
     */
    public LongLongHashMap() {
        this(8);
    }

    /**
     * Creates a map sized for at least {@code expectedSize} entries without
     * resizing, assuming the configured load factor.
     *
     * @param expectedSize expected number of entries; must not be negative
     */
    public LongLongHashMap(int expectedSize) {
        if (expectedSize < 0) {
            throw new IllegalArgumentException("expectedSize must not be negative");
        }

        int capacity = capacityFor(expectedSize);
        this.keys = new long[capacity];
        this.values = new long[capacity];
        this.occupied = new boolean[capacity];
        this.mask = capacity - 1;
        this.maxFill = maxFill(capacity);
    }

    /**
     * Returns the number of key/value pairs in this map.
     */
    public int size() {
        return size;
    }

    /**
     * Returns {@code true} if this map contains no entries.
     */
    public boolean isEmpty() {
        return size == 0;
    }

    /**
     * Returns {@code true} if {@code key} is present.
     */
    public boolean containsKey(long key) {
        return occupied[findIndex(key)];
    }

    /**
     * Returns the value associated with {@code key}, or zero if it is absent.
     */
    public long get(long key) {
        return getOrDefault(key, 0L);
    }

    /**
     * Returns the value associated with {@code key}, or {@code defaultValue}
     * if it is absent.
     */
    public long getOrDefault(long key, long defaultValue) {
        int index = findIndex(key);
        return occupied[index] ? values[index] : defaultValue;
    }

    /**
     * Associates {@code value} with {@code key}.
     *
     * @return the previous value, or zero if the key was not present
     */
    public long put(long key, long value) {
        int index = findIndex(key);
        if (occupied[index]) {
            long previous = values[index];
            values[index] = value;
            return previous;
        }

        ensureCapacityForOneMore();
        index = findIndex(key);
        keys[index] = key;
        values[index] = value;
        occupied[index] = true;
        size++;
        return 0L;
    }

    /**
     * Adds {@code increment} to the value associated with {@code key}.
     * A missing key is inserted with {@code increment} as its value.
     *
     * @return the updated value
     */
    public long addTo(long key, long increment) {
        int index = findIndex(key);
        if (occupied[index]) {
            values[index] += increment;
            return values[index];
        }

        ensureCapacityForOneMore();
        index = findIndex(key);
        keys[index] = key;
        values[index] = increment;
        occupied[index] = true;
        size++;
        return increment;
    }

    /**
     * Removes {@code key} from the map.
     *
     * @return the previous value, or zero if the key was absent
     */
    public long remove(long key) {
        int index = findIndex(key);
        if (!occupied[index]) {
            return 0L;
        }

        long previous = values[index];
        shiftKeys(index);
        size--;
        return previous;
    }

    /**
     * Removes all entries while retaining the current table capacity.
     */
    public void clear() {
        Arrays.fill(occupied, false);
        Arrays.fill(values, 0L);
        size = 0;
    }

    /**
     * Visits every entry without allocating entry objects.
     *
     * <p>The order is unspecified. The map must not be structurally modified
     * from inside the callback.</p>
     */
    public void forEach(LongLongConsumer consumer) {
        Objects.requireNonNull(consumer, "consumer");
        for (int i = 0; i < keys.length; i++) {
            if (occupied[i]) {
                consumer.accept(keys[i], values[i]);
            }
        }
    }

    /**
     * Keeps only the {@code maxEntries} entries with the greatest values,
     * removing the rest and shrinking the backing arrays to fit the retained
     * entries.
     *
     * <p>Ties are resolved deterministically: for equal values, the smaller
     * key is retained first. This makes the retained set independent of table
     * iteration order.</p>
     *
     * @param maxEntries maximum number of entries to retain; must not be
     *                   negative
     * @return the number of entries removed
     */
    public int retainTopK(int maxEntries) {
        if (maxEntries < 0) {
            throw new IllegalArgumentException("maxEntries must not be negative");
        }
        if (size <= maxEntries) {
            return 0;
        }

        int oldSize = size;
        if (maxEntries == 0) {
            keys = new long[MIN_CAPACITY];
            values = new long[MIN_CAPACITY];
            occupied = new boolean[MIN_CAPACITY];
            mask = MIN_CAPACITY - 1;
            maxFill = maxFill(MIN_CAPACITY);
            size = 0;
            return oldSize;
        }

        long[] heapKeys = new long[maxEntries];
        long[] heapValues = new long[maxEntries];
        int heapSize = 0;

        for (int i = 0; i < keys.length; i++) {
            if (!occupied[i]) {
                continue;
            }

            long key = keys[i];
            long value = values[i];
            if (heapSize < maxEntries) {
                heapKeys[heapSize] = key;
                heapValues[heapSize] = value;
                siftUpWorstFirst(heapKeys, heapValues, heapSize);
                heapSize++;
            } else if (isBetter(value, key, heapValues[0], heapKeys[0])) {
                heapKeys[0] = key;
                heapValues[0] = value;
                siftDownWorstFirst(heapKeys, heapValues, heapSize, 0);
            }
        }

        int capacity = capacityFor(maxEntries);
        keys = new long[capacity];
        values = new long[capacity];
        occupied = new boolean[capacity];
        mask = capacity - 1;
        maxFill = maxFill(capacity);
        size = 0;

        for (int i = 0; i < heapSize; i++) {
            insertWithoutResize(heapKeys[i], heapValues[i]);
        }

        return oldSize - size;
    }

    private void ensureCapacityForOneMore() {
        if (size + 1 <= maxFill) {
            return;
        }
        if (keys.length == MAX_CAPACITY) {
            throw new IllegalStateException("Maximum hash map capacity reached");
        }
        rehash(keys.length << 1);
    }

    private void rehash(int newCapacity) {
        long[] oldKeys = keys;
        long[] oldValues = values;
        boolean[] oldOccupied = occupied;

        keys = new long[newCapacity];
        values = new long[newCapacity];
        occupied = new boolean[newCapacity];
        mask = newCapacity - 1;
        maxFill = maxFill(newCapacity);
        size = 0;

        for (int i = 0; i < oldKeys.length; i++) {
            if (oldOccupied[i]) {
                insertWithoutResize(oldKeys[i], oldValues[i]);
            }
        }
    }

    private void insertWithoutResize(long key, long value) {
        int index = mix(key) & mask;
        while (occupied[index]) {
            index = (index + 1) & mask;
        }
        keys[index] = key;
        values[index] = value;
        occupied[index] = true;
        size++;
    }

    private int findIndex(long key) {
        int index = mix(key) & mask;
        while (occupied[index] && keys[index] != key) {
            index = (index + 1) & mask;
        }
        return index;
    }

    /**
     * Backward-shift deletion for linear probing. This avoids tombstones and
     * keeps lookups bounded by the occupied cluster.
     */
    private void shiftKeys(int position) {
        int last;
        int slot;
        long key = 0L;

        while (true) {
            position = ((last = position) + 1) & mask;
            while (occupied[position]) {
                key = keys[position];
                slot = mix(key) & mask;
                if (last <= position
                        ? last >= slot || slot > position
                        : last >= slot && slot > position) {
                    break;
                }
                position = (position + 1) & mask;
            }

            if (!occupied[position]) {
                occupied[last] = false;
                values[last] = 0L;
                return;
            }

            keys[last] = key;
            values[last] = values[position];
            occupied[last] = true;
        }
    }

    private static void siftUpWorstFirst(long[] heapKeys, long[] heapValues, int index) {
        while (index > 0) {
            int parent = (index - 1) >>> 1;
            if (!isWorse(heapValues[index], heapKeys[index],
                    heapValues[parent], heapKeys[parent])) {
                break;
            }
            swap(heapKeys, heapValues, index, parent);
            index = parent;
        }
    }

    private static void siftDownWorstFirst(long[] heapKeys, long[] heapValues,
                                           int heapSize, int index) {
        while (true) {
            int left = (index << 1) + 1;
            if (left >= heapSize) {
                return;
            }

            int right = left + 1;
            int worseChild = left;
            if (right < heapSize
                    && isWorse(heapValues[right], heapKeys[right],
                    heapValues[left], heapKeys[left])) {
                worseChild = right;
            }

            if (!isWorse(heapValues[worseChild], heapKeys[worseChild],
                    heapValues[index], heapKeys[index])) {
                return;
            }

            swap(heapKeys, heapValues, index, worseChild);
            index = worseChild;
        }
    }

    private static void swap(long[] heapKeys, long[] heapValues, int a, int b) {
        long key = heapKeys[a];
        heapKeys[a] = heapKeys[b];
        heapKeys[b] = key;

        long value = heapValues[a];
        heapValues[a] = heapValues[b];
        heapValues[b] = value;
    }

    /**
     * True when candidate A ranks ahead of candidate B.
     */
    private static boolean isBetter(long valueA, long keyA, long valueB, long keyB) {
        return valueA > valueB || (valueA == valueB && keyA < keyB);
    }

    /**
     * True when A is worse than B and should be nearer the heap root.
     */
    private static boolean isWorse(long valueA, long keyA, long valueB, long keyB) {
        return valueA < valueB || (valueA == valueB && keyA > keyB);
    }

    private static int capacityFor(int expectedSize) {
        long needed = Math.max(MIN_CAPACITY,
                (long) Math.ceil(expectedSize / (double) LOAD_FACTOR));
        int capacity = MIN_CAPACITY;
        while (capacity < needed) {
            if (capacity >= MAX_CAPACITY) {
                throw new IllegalArgumentException("expectedSize is too large");
            }
            capacity <<= 1;
        }
        return capacity;
    }

    private static int maxFill(int capacity) {
        return Math.min(capacity - 1, (int) (capacity * LOAD_FACTOR));
    }

    /**
     * 64-bit avalanche mixer; the low bits are suitable for power-of-two tables.
     */
    private static int mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdL;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53L;
        value ^= value >>> 33;
        return (int) value;
    }

    @FunctionalInterface
    public interface LongLongConsumer {
        void accept(long key, long value);
    }
}
