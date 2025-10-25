package rainfallPrediction;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * ${user}blackcontractor@farid
 */
public class PredictionMetrics {
    public final double rmse;
    public final double mae;
    public final double mape;
    public final double medianAbsError;
    public final double explainedVariance;

    public PredictionMetrics(double rmse, double mae, double mape, double medianAbsError, double explainedVariance) {
        this.rmse = rmse;
        this.mae = mae;
        this.mape = mape;
        this.medianAbsError = medianAbsError;
        this.explainedVariance = explainedVariance;
    }

    /**
     * Computes regression metrics between two lists of values.
     * @param actuals List of actual values.
     * @param predictions List of predicted values.
     * @return PredictionMetrics object containing RMSE, MAE, MAPE, Median Abs Error, and Explained Variance.
     * @throws IllegalArgumentException if lists are null, empty, or of unequal lengths.
     */
    public static PredictionMetrics evaluate(List<Double> actuals, List<Double> predictions) {
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
        double sumAbsPct = 0.0;
        int countNonZeroActual = 0;
        double[] absErrors = new double[n];
        double[] residuals = new double[n];

        // Calculate mean of actuals
        for (double act : actuals) {
            sumActual += act;
        }
        double meanActual = sumActual / n;

        // Calculate all metrics
        for (int i = 0; i < n; i++) {
            double actual = actuals.get(i);
            double pred = predictions.get(i);
            double err = pred - actual;

            sumSq += err * err;
            sumAbs += Math.abs(err);
            absErrors[i] = Math.abs(err);
            residuals[i] = actual - pred;
            if (actual != 0) {
                sumAbsPct += Math.abs(err / actual);
                countNonZeroActual++;
            }
        }

        // RMSE
        double rmse = Math.sqrt(sumSq / n);
        // MAE
        double mae = sumAbs / n;
        // MAPE (as percentage, divide by count of non-zero actuals)
        double mape = (countNonZeroActual == 0) ? Double.NaN : (sumAbsPct / countNonZeroActual) * 100.0;
        // Median Absolute Error
        double medianAbsError = median(absErrors);

        // Explained Variance: 1 - Var{residuals} / Var{actuals}
        double varActual = variance(actuals, meanActual);
        double varResidual = variance(residuals, mean(residuals));
        double explainedVariance = (varActual == 0) ? 1.0 : 1.0 - (varResidual / varActual);

        return new PredictionMetrics(rmse, mae, mape, medianAbsError, explainedVariance);
    }

    private static double mean(double[] arr) {
        double sum = 0.0;
        for (double v : arr) sum += v;
        return sum / arr.length;
    }

    private static double variance(List<Double> values, double mean) {
        double sum = 0.0;
        for (double v : values) sum += (v - mean) * (v - mean);
        return sum / values.size();
    }

    private static double variance(double[] arr, double mean) {
        double sum = 0.0;
        for (double v : arr) sum += (v - mean) * (v - mean);
        return sum / arr.length;
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