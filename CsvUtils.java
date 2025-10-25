package rainfallPrediction;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * ${user}blackcontractor@farid
 */
public class CsvUtils {
    public static void saveTableToCsv(File file, List<String> headers, List<RainfallPredictionApp.RainfallData> data) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write header
            for (int i = 0; i < headers.size(); i++) {
                writer.append(headers.get(i));
                writer.append(',');
            }
            writer.append("Prediction\n");
            // Write data
            for (RainfallPredictionApp.RainfallData row : data) {
                for (int i = 0; i < headers.size(); i++) {
                    writer.append(String.valueOf(row.getValue(i)));
                    writer.append(',');
                }
                writer.append(row.getPrediction());
                writer.append('\n');
            }
        }
    }
}
