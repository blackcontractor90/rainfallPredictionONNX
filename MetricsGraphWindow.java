package rainfallPrediction;

import javafx.embed.swing.SwingFXUtils;
import javafx.scene.Scene;
import javafx.scene.SnapshotParameters;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.scene.image.WritableImage;

import javax.imageio.ImageIO;
import java.io.File;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Displays a simple comparison graph of actual vs predicted rainfall values.
 * No metrics or extra UI.
 */
/**
 * ${user}blackcontractor@farid
 */
public class MetricsGraphWindow extends Stage {

    private static final AtomicInteger imageCounter = new AtomicInteger(1);

    public MetricsGraphWindow(List<RainfallPredictionApp.RainfallData> data) {
        setTitle("Actual vs Predicted Rainfall");

        NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Sample Index");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Rainfall (mm)");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Actual vs Predicted Rainfall");

        XYChart.Series<Number, Number> actualSeries = new XYChart.Series<>();
        actualSeries.setName("Actual");
        XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("Predicted");

        int index = 0;
        for (RainfallPredictionApp.RainfallData row : data) {
            Float actual = row.rainfallTarget;
            String predStr = row.getPrediction();
            if (actual != null && isNumeric(predStr)) {
                double predicted = Double.parseDouble(predStr.replaceAll("[^0-9.\\-Ee]", ""));
                actualSeries.getData().add(new XYChart.Data<>(index, actual));
                predictedSeries.getData().add(new XYChart.Data<>(index, predicted));
                index++;
            }
        }

        chart.getData().addAll(actualSeries, predictedSeries);

        Button saveImageBtn = new Button("Save as Image");
        saveImageBtn.setOnAction(e -> saveChartAsImage(chart));

        VBox root = new VBox(10, chart, saveImageBtn);
        root.setStyle("-fx-padding: 12; -fx-background-color: white;");

        setScene(new Scene(root, 800, 600));
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

    private void saveChartAsImage(LineChart<Number, Number> chart) {
        WritableImage image = chart.snapshot(new SnapshotParameters(), null);
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Chart as Image");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("PNG Image", "*.png")
        );
        int count = imageCounter.getAndIncrement();
        fileChooser.setInitialFileName("chart_" + count + ".png");
        File file = fileChooser.showSaveDialog(this);
        if (file != null) {
            try {
                ImageIO.write(SwingFXUtils.fromFXImage(image, null), "png", file);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }
}