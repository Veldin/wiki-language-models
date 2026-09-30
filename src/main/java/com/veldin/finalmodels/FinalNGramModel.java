package com.veldin.finalmodels;

import com.veldin.ngrampackstrategy.NGramPackStrategy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class FinalNGramModel {

    private final Map<Integer, Short> predictions;
    private final NGramPackStrategy strategy;

    public FinalNGramModel(
            Map<Integer, Short> predictions,
            NGramPackStrategy strategy
    ) {
        this.predictions = predictions;
        this.strategy = strategy;
    }

    /*
     * Return the best prediction.
     */
    public int predict(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1
    ) {

        return getFirst(
                previous8,
                previous7,
                previous6,
                previous5,
                previous4,
                previous3,
                previous2,
                previous1
        );
    }

    /*
     * Return all predictions for this context.
     */
    public List<Integer> predictAll(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1
    ) {

        List<Integer> result =
                new ArrayList<>(2);

        addPrediction(result, getFirst(
                previous8,
                previous7,
                previous6,
                previous5,
                previous4,
                previous3,
                previous2,
                previous1
        ));
        addPrediction(result, getSecond(
                previous8,
                previous7,
                previous6,
                previous5,
                previous4,
                previous3,
                previous2,
                previous1
        ));

        return result;
    }

    /*
     * Return the first / most likely prediction.
     */
    private int getFirst(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1
    ) {

        Short value =
                predictions.get(strategy.pack(
                        previous8,
                        previous7,
                        previous6,
                        previous5,
                        previous4,
                        previous3,
                        previous2,
                        previous1
                ));

        if (value == null) {
            return -1;
        }

        return value & 0xFF;
    }

    /*
     * Return the second most likely prediction.
     *
     * 255 means that no second prediction exists.
     */
    private int getSecond(
            int previous8,
            int previous7,
            int previous6,
            int previous5,
            int previous4,
            int previous3,
            int previous2,
            int previous1
    ) {

        Short value =
                predictions.get(strategy.pack(
                        previous8,
                        previous7,
                        previous6,
                        previous5,
                        previous4,
                        previous3,
                        previous2,
                        previous1
                ));

        if (value == null) {
            return -1;
        }

        int second =
                (value >>> 8) & 0xFF;

        return second == 255
                ? -1
                : second;
    }

    private void addPrediction(
            List<Integer> predictions,
            int prediction
    ) {
        if (prediction != -1
                && !predictions.contains(prediction)) {

            predictions.add(prediction);
        }
    }

    public Map<Integer, Short> getPredictions() {
        return predictions;
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(
                predictions,
                strategy
        );
    }
}