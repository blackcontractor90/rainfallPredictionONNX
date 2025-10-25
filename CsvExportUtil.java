package rainfallPrediction;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * ${user}blackcontractor@farid
 */
public class CsvExportUtil {
    public static void saveAsCsv(
            List<RainfallPredictionApp.RainfallData> data,
            List<String> featureHeaders,
            String filename
    ) throws IOException {
        try (FileWriter writer = new FileWriter(filename)) {
            // Write header without trailing comma
            for (int i = 0; i < featureHeaders.size(); i++) {
                writer.append(featureHeaders.get(i));
                if (i != featureHeaders.size() - 1) writer.append(",");
            }
            writer.append("\n");
            for (RainfallPredictionApp.RainfallData row : data) {
                for (int i = 0; i < row.getFeatures().size(); i++) {
                    writer.append(String.valueOf(row.getValue(i))).append(",");
                }
                // Write actual and prediction columns
                writer.append(row.rainfallTarget == null ? "" : row.rainfallTarget.toString()).append(",");
                writer.append(row.getPrediction()).append("\n");
            }
        }
    }
}