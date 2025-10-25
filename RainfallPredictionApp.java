package rainfallPrediction;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import javax.imageio.ImageIO;

public class RainfallPredictionApp extends Application {

    // Matches ONNX/Python one-hot order
    public static final List<String> STATE_LIST = Arrays.asList(
        "Johor", "Kedah", "Kelantan", "Melaka", "Pahang", "Perak", "Perlis",
        "Pulau Pinang", "Sabah", "Sarawak", "Selangor", "Terengganu", "Wilayah Persekutuan Labuan"
    );

    // 4 numeric + 13 state one-hot = 17
    public static final int FEATURE_SIZE = 17;

    private final ObservableList<RainfallData> data = FXCollections.observableArrayList();
    private final TableView<RainfallData> tableView = new TableView<>();
    private RainfallModelService modelService;
    private Button predictBtn;
    private Button clearBtn;
    private Button graphBtn;
    private Button metricsBtn;
    private Button saveCsvBtn;
    private Button compareCsvBtn;
    private Label modelStatus;
    private ProgressBar progressBar;
    private boolean dataLoaded = false;

    private static AtomicInteger chartSaveCounter = new AtomicInteger(1);

    public static void main(String[] args) {
        launch(args);
    }

    /**
     *
     */
    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Rainfall Prediction");
        BorderPane root = new BorderPane();

        HBox controlPanel = createControlPanel(primaryStage);
        root.setTop(controlPanel);

        VBox centerPanel = new VBox();
        centerPanel.getChildren().add(new ScrollPane(tableView));

        HBox statusBar = createStatusBar();
        root.setBottom(statusBar);

        root.setCenter(centerPanel);
        primaryStage.setScene(new Scene(root, 900, 600));
        primaryStage.show();
    }
    
    /**
     * Manual test mode to directly run a prediction using a known feature vector (copied from Python).
     * Uncomment in main() for debug. Useful for pipeline validation/debug.
     */
    public static void runManualTestPrediction() {
        try {
            RainfallModelService service = new RainfallModelService();
            String modelPath = "msiarainfallmodel.onnx"; // Adjust path if needed
            String loadResult = service.loadModel(new File(modelPath));
            if (loadResult != null) {
                System.err.println("Model load failed: " + loadResult);
                return;
            }
            // === Paste the feature vector from Python's PYTHON RAW FEATURES (convert True/False to 1.0f/0.0f) ===
            List<Float> features = Arrays.asList(
                37.8f, 22.9f, 32.3f, 86.1f,
                1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f, 0.0f
            );
            String prediction = service.predict(features);
            System.out.println("Manual test prediction = " + prediction);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    private HBox createControlPanel(Stage stage) {
        Button loadModelBtn = new Button("Load Model");
        Button loadDataBtn = new Button("Load CSV");
        predictBtn = new Button("Predict");
        predictBtn.setDisable(true);

        clearBtn = new Button("Clear Data");
        clearBtn.setDisable(true);

        graphBtn = new Button("Show Prediction Graph");
        graphBtn.setDisable(true);

        metricsBtn = new Button("Show Metrics Graph");
        metricsBtn.setDisable(true);

        saveCsvBtn = new Button("Save as CSV");
        saveCsvBtn.setDisable(true);

        compareCsvBtn = new Button("Compare Metrics from CSV");
        compareCsvBtn.setOnAction(e -> compareMetricsFromCsv(stage));

        loadModelBtn.setOnAction(e -> loadModel(stage));
        loadDataBtn.setOnAction(e -> loadCSV(stage));
        predictBtn.setOnAction(e -> predict());
        clearBtn.setOnAction(e -> clearData());
        graphBtn.setOnAction(e -> showPredictionGraph());
        //metricsBtn.setOnAction(e -> showMetricsGraph());
        metricsBtn.setOnAction(e -> {
            RainfallGraphGenerator.chooseAndShowRainfallChart(
                (Stage) metricsBtn.getScene().getWindow(),
                "Actual",         // Replace with the actual column name in your CSV
                "Prediction"      // Replace with the predicted column name in your CSV
            );
        });
        saveCsvBtn.setOnAction(e -> saveAsCsv());

        HBox hbox = new HBox(10);
        hbox.setPadding(new Insets(10));
        hbox.getChildren().addAll(
            loadModelBtn, loadDataBtn, predictBtn, clearBtn, graphBtn, metricsBtn, saveCsvBtn, compareCsvBtn
        );
        return hbox;
    }

    private HBox createStatusBar() {
        modelStatus = new Label("Model: Not Loaded");
        modelStatus.setStyle("-fx-text-fill: red; -fx-font-weight: bold;");

        progressBar = new ProgressBar(0);
        progressBar.setVisible(false);
        progressBar.setPrefWidth(200);

        HBox statusBar = new HBox(10);
        statusBar.setPadding(new Insets(5));
        statusBar.getChildren().addAll(modelStatus, progressBar);
        return statusBar;
    }

    private void loadModel(Stage stage) {
        if (modelService != null) modelService.close();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.setTitle("Select Model File");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("ONNX Models", "*.onnx"),
                new FileChooser.ExtensionFilter("DJL Models", "*.*")
        );
        File modelFile = fileChooser.showOpenDialog(stage);

        if (modelFile != null) {
            modelService = new RainfallModelService();
            String error = modelService.loadModel(modelFile);
            if (error == null) {
                modelStatus.setText("Model: Loaded (" + modelFile.getName() + ")");
                modelStatus.setStyle("-fx-text-fill: green; -fx-font-weight: bold;");
                showAlert("Success", "Model loaded successfully!");
                enablePredictIfReady();
            } else {
                modelStatus.setText("Model: Error - See console");
                showAlert("Model Load Error", error);
            }
        }
    }

    private void loadCSV(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setInitialDirectory(new File(System.getProperty("user.home")));
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            CsvDataLoader csvLoader = new CsvDataLoader();
            CsvDataLoader.CsvResult result = csvLoader.parseCsv(file);

            // Find all column indices
            int heightIdx = -1, minMeanTempIdx = -1, maxMeanTempIdx = -1, meanRelHumIdx = -1, stateIdx = -1, targetIdx = -1;
            List<String> colNames = result.columns;
            for (int i = 0; i < colNames.size(); i++) {
                String col = colNames.get(i).trim();
                if (col.equalsIgnoreCase("height")) heightIdx = i;
                else if (col.equalsIgnoreCase("minMeanTemp")) minMeanTempIdx = i;
                else if (col.equalsIgnoreCase("maxMeanTemp")) maxMeanTempIdx = i;
                else if (col.equalsIgnoreCase("meanRelHum")) meanRelHumIdx = i;
                else if (col.equalsIgnoreCase("state")) stateIdx = i;
                else if (col.equalsIgnoreCase("rainfall")) targetIdx = i;
            }
            if (heightIdx == -1 || minMeanTempIdx == -1 || maxMeanTempIdx == -1 || meanRelHumIdx == -1 || stateIdx == -1) {
                showAlert("Error", "CSV missing one or more required columns.");
                return;
            }
            List<RainfallData> processedRows = new ArrayList<>();
            for (RainfallData row : result.rows) {
                // Build the feature vector in the ONNX order (only 17 features)
                List<Float> rowData = new ArrayList<>();
                rowData.add(row.getValue(heightIdx));
                rowData.add(row.getValue(minMeanTempIdx));
                rowData.add(row.getValue(maxMeanTempIdx));
                rowData.add(row.getValue(meanRelHumIdx));
                String stateName = row.originalState != null ? row.originalState : "Unknown";
                // One-hot for state (ONNX order!)
                for (String s : STATE_LIST) {
                    rowData.add(stateName.equals(s) ? 1.0f : 0.0f);
                }
                // Keep actual target for metrics graph
                processedRows.add(new RainfallData(rowData, stateName, row.rainfallTarget));
            }
            // Build table headers
            List<String> featureHeaders = new ArrayList<>(Arrays.asList(
                "height", "minMeanTemp", "maxMeanTemp", "meanRelHum"
            ));
            for (String s : STATE_LIST) featureHeaders.add("state_" + s);
            setupTableColumns(featureHeaders);
            data.clear();
            data.addAll(processedRows);
            tableView.setItems(data);
            showAlert("Data Loaded", "Loaded " + data.size() + " records");
            dataLoaded = true;
            clearBtn.setDisable(false);
            saveCsvBtn.setDisable(false);
            metricsBtn.setDisable(false);
            graphBtn.setDisable(false);
            enablePredictIfReady();
        }
    }

    private void setupTableColumns(List<String> headers) {
        tableView.getColumns().clear();
        for (int i = 0; i < Math.min(headers.size(), FEATURE_SIZE); i++) {
            final int colIndex = i;
            TableColumn<RainfallData, String> col = new TableColumn<>(headers.get(i));
            col.setCellValueFactory(cellData -> {
                float v = cellData.getValue().getValue(colIndex);
                return new SimpleStringProperty(Float.isFinite(v) ? String.format("%.2f", v) : "-");
            });
            tableView.getColumns().add(col);
        }
        TableColumn<RainfallData, String> predictionCol = new TableColumn<>("Prediction");
        predictionCol.setCellValueFactory(cellData -> cellData.getValue().predictionProperty());
        tableView.getColumns().add(predictionCol);
    }

    private void predict() {
        if (!isReadyForPrediction()) {
            showAlert("Error", "Please load both a model and data.");
            return;
        }
        predictBtn.setDisable(true);
        progressBar.setVisible(true);
        progressBar.setProgress(0);

        Task<Void> predictionTask = new Task<>() {
            @Override
            protected Void call() {
                int totalRows = data.size();
                for (int i = 0; i < totalRows; i++) {
                    RainfallData row = data.get(i);
                    try {
                        String prediction = modelService.predict(row.getFeatures());
                        final String formatted = String.format("%.2f", Double.parseDouble(prediction.replaceAll("[^0-9.\\-Ee]", "")));
                        Platform.runLater(() -> row.setPrediction(formatted));
                    } catch (Exception e) {
                        Platform.runLater(() -> showAlert("Error", "Prediction failed: " + e.getMessage()));
                    }
                    updateProgress(i + 1, totalRows);
                }
                Platform.runLater(() -> {
                    saveCsvBtn.setDisable(false);
                    metricsBtn.setDisable(false);
                    graphBtn.setDisable(false);
                });
                return null;
            }
        };

        progressBar.progressProperty().bind(predictionTask.progressProperty());

        predictionTask.setOnSucceeded(e -> {
            progressBar.setVisible(false);
            predictBtn.setDisable(false);
        });
        predictionTask.setOnFailed(e -> {
            progressBar.setVisible(false);
            predictBtn.setDisable(false);
            showAlert("Prediction Error", "Prediction task failed.");
        });

        new Thread(predictionTask).start();
    }

    private void clearData() {
        data.clear();
        dataLoaded = false;
        clearBtn.setDisable(true);
        predictBtn.setDisable(true);
        graphBtn.setDisable(true);
        saveCsvBtn.setDisable(true);
        metricsBtn.setDisable(true);
        tableView.getColumns().clear();
        if (modelService != null) {
            modelService.getPredictionHistory().clear();
        }
    }

    private void enablePredictIfReady() {
        predictBtn.setDisable(!(isReadyForPrediction()));
    }

    private boolean isReadyForPrediction() {
        return modelService != null && modelService.isLoaded() && dataLoaded && !data.isEmpty();
    }

    @Override
    public void stop() {
        if (modelService != null) modelService.close();
        System.out.println("Application closed - resources released");
    }

    private void showAlert(String title, String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    // --- Metrics Graph Implementation with PredictionMetrics ---
    private void showMetricsGraph() {
        if (data.isEmpty()) {
            showAlert("No Data", "No data to plot.");
            return;
        }
        // Collect actuals and predicted values for rows that have both
        List<Double> actuals = new ArrayList<>();
        List<Double> preds = new ArrayList<>();
        for (RainfallData row : data) {
            if (row.rainfallTarget != null && isNumeric(row.getPrediction())) {
                actuals.add(row.rainfallTarget.doubleValue());
                preds.add(Double.parseDouble(row.getPrediction().replaceAll("[^0-9.\\-Ee]", "")));
            }
        }
        if (actuals.isEmpty()) {
            showAlert("No Data", "No actual vs. predicted data to evaluate.");
            return;
        }
        // Use your PredictionMetrics class for evaluation
        PredictionMetrics metrics = PredictionMetrics.evaluate(actuals, preds);

        // Show the metrics graph window with metrics
        MetricsGraphWindow metricsGraph = new MetricsGraphWindow(data);
        metricsGraph.show();

        // Show metrics in a dialog
        String msg = String.format(
            "RMSE: %.4f\nMAE: %.4f\nMAPE: %.2f%%\nMedian Abs Error: %.4f\nExplained Variance: %.4f",
            metrics.rmse, metrics.mae, metrics.mape, metrics.medianAbsError, metrics.explainedVariance
        );
        showAlert("Prediction Metrics", msg);
    }

    // --- Prediction Graph ---
    private void showPredictionGraph() {
        if (data.isEmpty()) {
            showAlert("No Data", "No predicted data to plot.");
            return;
        }
        PredictionGraphWindow predictionGraph = new PredictionGraphWindow(data);
        predictionGraph.show();
    }

    // --- Save as CSV Implementation ---
    private void saveAsCsv() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save as CSV");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showSaveDialog(tableView.getScene().getWindow());
        if (file != null) {
            List<String> featureHeaders = new ArrayList<>(Arrays.asList(
                "height", "minMeanTemp", "maxMeanTemp", "meanRelHum"
            ));
            for (String s : STATE_LIST) featureHeaders.add("state_" + s);
            // Add actual and prediction columns
            featureHeaders.add("Actual");
            featureHeaders.add("Prediction");
            try {
                CsvExportUtil.saveAsCsv(data, featureHeaders, file.getAbsolutePath());
                showAlert("Success", "CSV saved to: " + file.getAbsolutePath());
            } catch (Exception ex) {
                showAlert("Error", "Failed to save CSV: " + ex.getMessage());
            }
        }
    }

    // --- Compare Metrics from CSV Implementation ---
    private void compareMetricsFromCsv(Stage stage) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select CSV File for Metrics Comparison");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
        File file = fileChooser.showOpenDialog(stage);
        if (file == null) return;

        List<Double> actuals = new ArrayList<>();
        List<Double> preds = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String header = reader.readLine(); // skip header
            String line;
            int actualIdx = -2, predIdx = -1;
            String[] headers = header.split(",");
            // Find indices for "Actual" and "Prediction" (robust to order)
            for (int i = 0; i < headers.length; i++) {
                if (headers[i].trim().equalsIgnoreCase("Actual")) actualIdx = i;
                if (headers[i].trim().equalsIgnoreCase("Prediction")) predIdx = i;
            }
            if (actualIdx < 0 || predIdx < 0) {
                showAlert("Error", "CSV must have 'Actual' and 'Prediction' columns.");
                return;
            }
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length <= Math.max(actualIdx, predIdx)) continue;
                String actualStr = parts[actualIdx];
                String predStr = parts[predIdx];
                if (isNumeric(actualStr) && isNumeric(predStr)) {
                    actuals.add(Double.parseDouble(actualStr));
                    preds.add(Double.parseDouble(predStr));
                }
            }
            if (actuals.isEmpty()) {
                showAlert("No valid data", "The CSV file does not contain valid actual and prediction columns.");
                return;
            }
            PredictionMetrics metrics = PredictionMetrics.evaluate(actuals, preds);
            String msg = String.format(
                "RMSE: %.4f\nMAE: %.4f\nMAPE: %.2f%%\nMedian Abs Error: %.4f\nExplained Variance: %.4f",
                metrics.rmse, metrics.mae, metrics.mape, metrics.medianAbsError, metrics.explainedVariance
            );
            showAlert("Metrics from CSV", msg);
        } catch (Exception ex) {
            showAlert("Error", "Failed to read or parse CSV: " + ex.getMessage());
        }
    }
    // different implementation of reading & generating chart
    private void showAndSaveMetricsChartWindow() {
        // Prepare data
        List<String> labels = new ArrayList<>();
        List<Double> actualValues = new ArrayList<>();
        List<Double> predictedValues = new ArrayList<>();
        int idx = 1;
        for (RainfallData row : data) {
            if (row.rainfallTarget != null && isNumeric(row.getPrediction())) {
                labels.add(String.valueOf(idx++));
                actualValues.add(row.rainfallTarget.doubleValue());
                predictedValues.add(Double.parseDouble(row.getPrediction().replaceAll("[^0-9.\\-Ee]", "")));
            }
        }
        if (labels.isEmpty()) {
            showAlert("No Data", "No actual vs. predicted data to plot.");
            return;
        }

        // Set up chart
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

        // Show in new window
        Stage chartStage = new Stage();
        chartStage.setTitle("Metrics Comparison Chart");
        chartStage.setScene(new Scene(lineChart, 900, 600));
        chartStage.show();

        // Save incrementally
        int count = 1;
        File file;
        do {
            file = new File(String.format("rainfall_comparison_%d.png", count++));
        } while (file.exists());

        WritableImage image = lineChart.snapshot(new SnapshotParameters(), null);
        try {
            ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            showAlert("Saved", "Metrics comparison graph saved as " + file.getName());
        } catch (Exception ex) {
            showAlert("Error", "Failed to save chart: " + ex.getMessage());
        }
    }
    
    private boolean isNumeric(String str) {
        if (str == null) return false;
        try {
            Double.parseDouble(str.replaceAll("[^0-9.\\-Ee]", ""));
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static class RainfallData {
        private final List<Float> rowData;
        private final StringProperty prediction = new SimpleStringProperty("Pending...");
        public final String originalState;
        public final Float rainfallTarget;

        public RainfallData(List<Float> rowData, String originalState, Float rainfallTarget) {
            this.rowData = rowData;
            this.originalState = originalState;
            this.rainfallTarget = rainfallTarget;
        }

        public List<Float> getFeatures() {
            return rowData;
        }

        public Float getValue(int index) {
            return index < rowData.size() ? rowData.get(index) : 0f;
        }

        public String getPrediction() {
            return prediction.get();
        }

        public void setPrediction(String value) {
            prediction.set(value);
        }

        public StringProperty predictionProperty() {
            return prediction;
        }
    }
}