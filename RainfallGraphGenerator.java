package rainfallPrediction;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.image.WritableImage;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.io.*;
import java.util.*;

/**
 * ${user}blackcontractor@farid
 */
public class RainfallGraphGenerator {

    /**
     * Opens a FileChooser, allows the user to select a CSV, and then displays and saves a rainfall comparison graph
     * using the given column names for actual and predicted rainfall.
     *
     * @param parentStage the parent JavaFX Stage for the dialog
     * @param actualCol   the column name for actual rainfall
     * @param predictedCol the column name for predicted rainfall
     */
    /**
     * @param parentStage
     * @param actualCol
     * @param predictedCol
     */
    public static void chooseAndShowRainfallChart(Stage parentStage, String actualCol, String predictedCol) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(parentStage);
        if (file != null) {
            showRainfallChartFromCSV(file, actualCol, predictedCol);
        }
    }

    public static void showRainfallChartFromCSV(File csvFile, String actualCol, String predictedCol) {
        List<String> labels = new ArrayList<>();
        List<Double> actualValues = new ArrayList<>();
        List<Double> predictedValues = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new IOException("CSV file is empty.");

            String[] headers = headerLine.split(",");
            int actualIdx = -1, predictedIdx = -1;
            // Find column indices by header name
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase(actualCol)) actualIdx = i;
                if (headers[i].trim().equalsIgnoreCase(predictedCol)) predictedIdx = i;
            }
            if (actualIdx < 0 || predictedIdx < 0)
                throw new IllegalArgumentException("Could not find specified columns in CSV header.");

            String line;
            int rowNum = 1;
            while ((line = reader.readLine()) != null) {
                String[] cells = line.split(",");
                if (cells.length <= Math.max(actualIdx, predictedIdx)) continue;
                String actualStr = cells[actualIdx];
                String predictedStr = cells[predictedIdx];
                if (isNumeric(actualStr) && isNumeric(predictedStr)) {
                    labels.add(String.valueOf(rowNum));
                    actualValues.add(Double.parseDouble(actualStr));
                    predictedValues.add(Double.parseDouble(predictedStr));
                }
                rowNum++;
            }
        } catch (Exception e) {
            Platform.runLater(() -> {
                showAlert("CSV Error", "Failed to read CSV: " + e.getMessage());
            });
            return;
        }

        if (labels.isEmpty()) {
            Platform.runLater(() -> showAlert("No Data", "No valid rows found in the CSV."));
            return;
        }

        Platform.runLater(() -> showChartWindow(labels, actualValues, predictedValues));
    }

    private static void showChartWindow(List<String> labels, List<Double> actualValues, List<Double> predictedValues) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Index");
        xAxis.setCategories(FXCollections.observableArrayList(labels));

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Rainfall (mm)");

        LineChart<String, Number> lineChart = new LineChart<>(xAxis, yAxis);
        lineChart.setTitle("Actual vs Predicted Rainfall");

        XYChart.Series<String, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual Rainfall");
        for (int i = 0; i < labels.size(); i++) {
            actualSeries.getData().add(new XYChart.Data<>(labels.get(i), actualValues.get(i)));
        }

        XYChart.Series<String, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("Predicted Rainfall");
        for (int i = 0; i < labels.size(); i++) {
            predictedSeries.getData().add(new XYChart.Data<>(labels.get(i), predictedValues.get(i)));
        }

        lineChart.getData().addAll(actualSeries, predictedSeries);

        Stage chartStage = new Stage();
        chartStage.setTitle("Rainfall Comparison Chart");
        chartStage.setScene(new Scene(lineChart, 900, 600));
        chartStage.show();

        // Save incrementally
        int count = 1;
        File imgFile;
        do {
            imgFile = new File(String.format("rainfall_comparison_%d.png", count++));
        } while (imgFile.exists());

        WritableImage image = lineChart.snapshot(new SnapshotParameters(), null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", imgFile);
            showAlert("Saved", "Rainfall chart saved as " + imgFile.getName());
        } catch (Exception ex) {
            showAlert("Error", "Failed to save chart: " + ex.getMessage());
        }
    }

    private static void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }

    private static boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str.replaceAll("[^0-9.\\-Ee]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}