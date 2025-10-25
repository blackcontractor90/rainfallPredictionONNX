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
 * ${user}blackcontractor@farid
 */
public class PredictionGraphWindow extends Stage {

    private static final AtomicInteger imageCounter = new AtomicInteger(1);

    public PredictionGraphWindow(List<RainfallPredictionApp.RainfallData> data) {
        setTitle("Predicted Rainfall");

        final NumberAxis xAxis = new NumberAxis();
        xAxis.setLabel("Sample Index");

        final NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Predicted Rainfall (mm)");

        LineChart<Number, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("Predicted Rainfall per Sample");

        XYChart.Series<Number, Number> predictedSeries = new XYChart.Series<>();
        predictedSeries.setName("Predicted Rainfall");

        int index = 0;
        for (RainfallPredictionApp.RainfallData row : data) {
            if (isNumeric(row.getPrediction())) {
                double predicted = Double.parseDouble(row.getPrediction().replaceAll("[^0-9.\\-Ee]", ""));
                predictedSeries.getData().add(new XYChart.Data<>(index, predicted));
                index++; // increment only for valid predictions
            }
        }

        chart.getData().add(predictedSeries);

        Button saveImageBtn = new Button("Save as Image");
        saveImageBtn.setOnAction(e -> saveChartAsImage(chart));

        VBox root = new VBox(chart, saveImageBtn);
        setScene(new Scene(root, 800, 640));
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
        // Suggest incremental filename
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