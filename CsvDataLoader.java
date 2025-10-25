package rainfallPrediction;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import rainfallPrediction.RainfallPredictionApp.RainfallData;

import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class CsvDataLoader {

    public static class CsvResult {
        public List<String> columns = new ArrayList<>();
        public List<RainfallData> rows = new ArrayList<>();
    }

    /**
     * Parses a CSV file robustly, extracting only the necessary 17 features for the ONNX model!
     * If the CSV has a "Prediction" column, also sets the prediction string for each row.
     */
    public CsvResult parseCsv(File file) {
        CsvResult result = new CsvResult();
        int skippedRows = 0;
        try {
            List<String> lines = Files.readAllLines(file.toPath());
            if (lines.isEmpty()) return result;

            // Split header and trim each column name
            result.columns = new ArrayList<>();
            for (String h : lines.get(0).split(",")) {
                result.columns.add(h.trim());
            }
            List<String> headers = result.columns;

            // Find column indices, including optional Prediction
            int heightIdx = -1, minMeanTempIdx = -1, maxMeanTempIdx = -1, meanRelHumIdx = -1, stateIdx = -1, targetIdx = -1, predIdx = -1;
            for (int i = 0; i < headers.size(); i++) {
                String col = headers.get(i);
                if (col.equalsIgnoreCase("height")) heightIdx = i;
                else if (col.equalsIgnoreCase("minMeanTemp")) minMeanTempIdx = i;
                else if (col.equalsIgnoreCase("maxMeanTemp")) maxMeanTempIdx = i;
                else if (col.equalsIgnoreCase("meanRelHum")) meanRelHumIdx = i;
                else if (col.equalsIgnoreCase("state")) stateIdx = i;
                else if (col.equalsIgnoreCase("rainfall") || col.equalsIgnoreCase("actual")) targetIdx = i;
                else if (col.equalsIgnoreCase("prediction")) predIdx = i;
            }
            if (heightIdx == -1 || minMeanTempIdx == -1 || maxMeanTempIdx == -1 ||
                meanRelHumIdx == -1 || stateIdx == -1) {
                alertLater("Error", "CSV missing one or more required columns (height, minMeanTemp, maxMeanTemp, meanRelHum, state). Headers: " + headers);
                return result;
            }

            int rowCount = 1;
            for (String line : lines.subList(1, lines.size())) {
                rowCount++;
                if (line.trim().isEmpty()) continue;
                String[] values = line.split(",", -1);

                if (values.length < headers.size()) {
                    System.out.println("DEBUG: Skipping row " + rowCount + " due to column count mismatch. Expected " + headers.size() + ", got " + values.length);
                    skippedRows++;
                    continue;
                }
                List<Float> features = new ArrayList<>();
                boolean validRow = true;
                try {
                    features.add(parseSafeFloat(values[heightIdx]));
                    features.add(parseSafeFloat(values[minMeanTempIdx]));
                    features.add(parseSafeFloat(values[maxMeanTempIdx]));
                    features.add(parseSafeFloat(values[meanRelHumIdx]));
                    String stateVal = values[stateIdx].trim();

                    // Extra diagnostic: print first 5 state values to catch typos
                    if (rowCount <= 6) System.out.println("DEBUG: Row " + rowCount + " stateVal='" + stateVal + "'");

                    // One-hot state (must match ONNX order)
                    boolean foundState = false;
                    for (String s : RainfallPredictionApp.STATE_LIST) {
                        if (stateVal.equals(s)) foundState = true;
                        features.add(stateVal.equals(s) ? 1.0f : 0.0f);
                    }
                    if (!foundState) {
                        System.out.println(
                            "DEBUG: Row " + rowCount + " WARNING: state '" + stateVal + "' not in STATE_LIST, will be encoded as all zeros."
                        );
                    }
                    if (features.size() != 17) {
                        System.out.println("DEBUG: Skipping row. Feature vector size is not 17: " + features.size());
                        validRow = false;
                    }
                    if (features.stream().anyMatch(f -> !Float.isFinite(f))) {
                        System.out.println("DEBUG: Skipping row. Non-finite value in features: " + features);
                        validRow = false;
                    }
                    Float target = (targetIdx != -1) ? parseSafeFloat(values[targetIdx]) : null;

                    // NEW: Get prediction string if present
                    String prediction = (predIdx != -1 && predIdx < values.length) ? values[predIdx] : null;

                    if (validRow) {
                        RainfallData row = new RainfallData(features, stateVal, target);
                        if (prediction != null && !prediction.isEmpty())
                            row.setPrediction(prediction);
                        result.rows.add(row);

                        if (rowCount <= 6) System.out.println("DEBUG: Added row with features.size() = " + features.size() + " - " + features + ", prediction=" + prediction);
                    } else {
                        skippedRows++;
                    }
                } catch (Exception ex) {
                    System.out.println("DEBUG: Exception on row " + rowCount + ": " + ex.getMessage());
                    skippedRows++;
                }
            }
        } catch (Exception e) {
            alertLater("Error", "File read error: " + e.getMessage());
        }
        if (skippedRows > 0) {
            alertLater("Info", "CsvDataLoader: Skipped " + skippedRows + " invalid row(s) with non-numeric or missing values.");
        }
        return result;
    }

    /**
     * Parses floats robustly: handles "-", "N/A", commas, empty, and units.
     */
    private static float parseSafeFloat(String val) {
        if (val == null) return Float.NaN;
        val = val.trim().replace(",", "")
                .replace("(", "")
                .replace(")", "")
                .replace("m", "");
        if (val.isEmpty() || val.equals("-") || val.equalsIgnoreCase("N/A")) return Float.NaN;
        try {
            return Float.parseFloat(val);
        } catch (NumberFormatException ex) {
            return Float.NaN;
        }
    }

    private void alertLater(String title, String msg) {
        Platform.runLater(() -> AlertHelper.showAlert(title, msg));
    }

    public static class AlertHelper {
        public static void showAlert(String title, String message) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        }
    }
}