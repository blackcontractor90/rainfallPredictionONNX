package rainfallPrediction;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ${user}blackcontractor@farid
 */
public class PredictionEvaluator {
    /**
     * Container for evaluation metrics.
     */
    public static class Metrics {
        public final double rmse;
        public final double mae;
        public final double mape;
        public final double medianAbsError;
        public final double explainedVariance;

        public Metrics(double rmse, double mae, double mape, double medianAbsError, double explainedVariance) {
            this.rmse = rmse;
            this.mae = mae;
            this.mape = mape;
            this.medianAbsError = medianAbsError;
            this.explainedVariance = explainedVariance;
        }
    }

    /**
     * Computes regression metrics between two lists of values.
     * @param actuals List of actual values.
     * @param predictions List of predicted values.
     * @return Metrics object containing RMSE, MAE, MAPE, Median Abs Error, and Explained Variance.
     * @throws IllegalArgumentException if lists are null, empty, or of unequal lengths.
     */
    public static Metrics evaluate(List<Double> actuals, List<Double> predictions) {
        Objects.requireNonNull(actuals, "Actuals list must not be null.");
        Objects.requireNonNull(predictions, "Predictions list must not be null.");
        if (actuals.size() != predictions.size()) {
            throw new IllegalArgumentException("Actuals and predictions must have the same length.");
        }
        if (actuals.isEmpty()) {
            throw new IllegalArgumentException("Input lists must not be empty.");
        }

        int n = actuals.size();
        double sumSq = 0.0;
        double sumAbs = 0.0;
        double sumActual = 0.0;
        double sumPred = 0.0;
        double meanActual = 0.0;
        double sumAbsPct = 0.0;
        double explainedVarNumer = 0.0;
        double explainedVarDenom = 0.0;

        // Prepare for median absolute error calculation
        double[] absErrors = new double[n];

        // Calculate means
        for (double act : actuals) {
            sumActual += act;
        }
        meanActual = sumActual / n;

        // Calculate all metrics
        for (int i = 0; i < n; i++) {
            double actual = actuals.get(i);
            double pred = predictions.get(i);
            double err = pred - actual;

            sumSq += err * err;
            sumAbs += Math.abs(err);
            absErrors[i] = Math.abs(err);
            if (actual != 0) {
                sumAbsPct += Math.abs(err / actual);
            }
            sumPred += pred;

            explainedVarNumer += (pred - meanActual) * (pred - meanActual);
            explainedVarDenom += (actual - meanActual) * (actual - meanActual);
        }

        // RMSE
        double rmse = Math.sqrt(sumSq / n);
        // MAE
        double mae = sumAbs / n;
        // MAPE (as percentage)
        double mape = (sumAbsPct / n) * 100.0;
        // Median Absolute Error
        double medianAbsError = median(absErrors);
        // Explained Variance
        double explainedVariance = (explainedVarDenom == 0) ? 1.0 : explainedVarNumer / explainedVarDenom;

        return new Metrics(rmse, mae, mape, medianAbsError, explainedVariance);
    }

    /**
     * Helper method to compute the median of an array.
     */
    private static double median(double[] arr) {
        double[] copy = arr.clone();
        java.util.Arrays.sort(copy);
        int n = copy.length;
        if (n % 2 == 1) {
            return copy[n / 2];
        } else {
            return (copy[(n / 2) - 1] + copy[n / 2]) / 2.0;
        }
    }
}