package com.veldin;

import java.util.Locale;

public final class Progress {

    private static final int DEFAULT_CHAR_WIDTH_LOADING_BAR = 50;

    private final String label;
    private final double total;
    private final String unit;
    private final int width;

    private final long startTime;

    private double completed;

    public Progress(
            String label,
            double total,
            String unit
    ) {
        this(
                label,
                total,
                unit,
                DEFAULT_CHAR_WIDTH_LOADING_BAR
        );
    }

    public Progress(
            String label,
            double total,
            String unit,
            int width
    ) {
        if (total <= 0) {
            throw new IllegalArgumentException(
                    "Total must be greater than zero."
            );
        }

        if (width <= 0) {
            throw new IllegalArgumentException(
                    "Width must be greater than zero."
            );
        }

        this.label = label;
        this.total = total;
        this.unit = unit;
        this.width = width;
        this.startTime = System.nanoTime();
    }

    public void next() {
        next(1);
    }

    public void next(double amount) {
        completed += amount;

        if (completed > total) {
            completed = total;
        }

        print();
    }

    public void print() {

        double seconds =
                (System.nanoTime() - startTime)
                        / 1_000_000_000.0;

        double percent =
                completed * 100.0 / total;

        double speed =
                seconds > 0
                        ? completed / seconds
                        : 0;

        int filled =
                (int) (
                        percent
                                / 100.0
                                * width
                );

        String bar =
                "=".repeat(
                        Math.max(0, filled)
                )
                        + " ".repeat(
                        Math.max(
                                0,
                                width - filled
                        )
                );

        System.out.printf(
                Locale.ROOT,
                "\r%s [%s] %.1f%% %,.1f/% ,.1f %s @ %,.1f %s/s",
                label,
                bar,
                percent,
                completed,
                total,
                unit,
                speed,
                unit
        );

        if (completed >= total) {
            System.out.println();
        }
    }

    public double completed() {
        return completed;
    }

    public double total() {
        return total;
    }

    public double remaining() {
        return total - completed;
    }

    public double percent() {
        return completed * 100.0 / total;
    }

    public boolean isComplete() {
        return completed >= total;
    }
}