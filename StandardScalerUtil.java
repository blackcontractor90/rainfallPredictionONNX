package rainfallPrediction;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class StandardScalerUtil {

    /**
     * Loads a CSV file (single row or column, comma separated) as a float array.
     * Used for scaler mean and scale files.
     */
    public static float[] loadArray(String path) throws Exception {
        List<String> lines = Files.readAllLines(Paths.get(path));
        List<Float> values = new ArrayList<>();
        for (String line : lines) {
            for (String part : line.split(",")) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) values.add(Float.parseFloat(trimmed));
            }
        }
        float[] arr = new float[values.size()];
        for (int i = 0; i < values.size(); i++) arr[i] = values.get(i);
        return arr;
    }

    /**
     * Applies standard scaling to the feature vector using provided mean and scale.
     * Prints debug information for easier matching with Python output.
     */
    public static float[] scale(List<Float> raw, float[] mean, float[] scale) {
        float[] out = new float[raw.size()];
        for (int i = 0; i < raw.size(); i++) {
            out[i] = (raw.get(i) - mean[i]) / scale[i];
        }

        // === DEBUG: Print each scaling operation ===
        System.out.println("STANDARD SCALER DEBUG:");
        for (int i = 0; i < raw.size(); i++) {
            System.out.printf(
                "  index %d: raw=%.6f, mean=%.6f, scale=%.6f, result=%.6f%n",
                i, raw.get(i), mean[i], scale[i], out[i]
            );
        }
        return out;
    }
}