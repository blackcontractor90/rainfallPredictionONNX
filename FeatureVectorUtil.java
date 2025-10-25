package rainfallPrediction;

import java.util.ArrayList;
import java.util.List;

public class FeatureVectorUtil {
    // Must match the order from feature_order.csv
    private static final String[] STATES = {
        "Johor",
        "Kedah",
        "Kelantan",
        "Melaka",
        "Pahang",
        "Perak",
        "Perlis",
        "Pulau Pinang",
        "Sabah",
        "Sarawak",
        "Selangor",
        "Terengganu",
        "Wilayah Persekutuan Labuan"
    };

    /**
     * Builds the feature vector in the correct order for ONNX inference.
     * 
     * @param height         numeric value
     * @param minMeanTemp    numeric value
     * @param maxMeanTemp    numeric value
     * @param meanRelHum     numeric value
     * @param state          state as a string, e.g. "Perak"
     * @return List<Float> of size 17, matching feature_order.csv
     */
    public static List<Float> buildFeatureVector(
            float height,
            float minMeanTemp,
            float maxMeanTemp,
            float meanRelHum,
            String state
    ) {
        List<Float> features = new ArrayList<>(17);
        features.add(height);
        features.add(minMeanTemp);
        features.add(maxMeanTemp);
        features.add(meanRelHum);
        for (String s : STATES) {
            features.add(s.equals(state) ? 1.0f : 0.0f);
        }
        return features;
    }
}