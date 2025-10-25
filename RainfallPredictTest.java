package rainfallPrediction;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class RainfallPredictTest {
    public static void main(String[] args) throws Exception {
        RainfallModelService service = new RainfallModelService();

        // Load the model (update the path as needed)
        File modelFile = new File("msiarainfallmodel.onnx");
        String loadResult = service.loadModel(modelFile);
        if (loadResult != null) {
            System.err.println("Failed to load model: " + loadResult);
            return;
        }

        // === Prepare the feature vector ===
        // Replace these values with your actual PYTHON RAW FEATURES, maintaining the same order!
        List<Float> features = Arrays.asList(
            37.8f, 22.9f, 32.3f, 86.1f,
            1f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f, 0f // Example one-hot encoding for 17 features
        );

        // Run prediction (debug output will print automatically)
        String prediction = service.predict(features);
        System.out.println("Rainfall prediction: " + prediction);
    }
}