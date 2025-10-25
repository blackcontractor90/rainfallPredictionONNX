package rainfallPrediction;

import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.translate.TranslateException;
import ai.djl.engine.Engine;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RainfallModelService {

    public static final int FEATURE_SIZE = 17;

    private ZooModel<NDList, NDList> model;
    private Predictor<NDList, NDList> predictor;

    private float[] scalerMean = null;
    private float[] scalerScale = null;

    // Store prediction history for graphing
    private List<Float> predictionHistory = new ArrayList<>();
    public List<Float> getPredictionHistory() { return predictionHistory; }

    public String loadModel(File modelFile) {
        close();
        try {
            Path modelPath = modelFile.toPath();
            String fileName = modelFile.getName().toLowerCase();

            Criteria<NDList, NDList> criteria;
            if (fileName.endsWith(".onnx")) {
                criteria = Criteria.builder()
                        .setTypes(NDList.class, NDList.class)
                        .optModelPath(modelPath)
                        .optEngine("OnnxRuntime")
                        .build();
            } else {
                criteria = Criteria.builder()
                        .setTypes(NDList.class, NDList.class)
                        .optModelPath(modelPath)
                        .build();
            }

            model = criteria.loadModel();
            predictor = model.newPredictor();

            // Load scaler parameters (assumes files are in project root or specify the correct path)
            try {
                scalerMean = StandardScalerUtil.loadArray("C:/Users/User/eclipse-workspace/RainfallONNX/scaler_mean.csv");
                scalerScale = StandardScalerUtil.loadArray("C:/Users/User/eclipse-workspace/RainfallONNX/scaler_scale.csv");
                if (scalerMean.length != FEATURE_SIZE || scalerScale.length != FEATURE_SIZE) {
                    scalerMean = null;
                    scalerScale = null;
                    throw new IllegalStateException("Scaler parameter size mismatch: mean=" +
                        (scalerMean != null ? scalerMean.length : "null") +
                        ", scale=" +
                        (scalerScale != null ? scalerScale.length : "null") +
                        ". Both must be of length " + FEATURE_SIZE);
                }
                System.out.println("Scaler parameters loaded successfully.");
            } catch (Exception e) {
                throw new RuntimeException("Failed to load scaler parameters (scaler_mean.csv, scaler_scale.csv): " + e.getMessage(), e);
            }

            System.out.println("Loaded model using engine: " + Engine.getInstance().getEngineName());
            System.out.println("Model path: " + modelPath);

            // Test the model with a dummy input (all zeros, scaled)
            try (NDManager manager = NDManager.newBaseManager()) {
                float[] testInput = new float[FEATURE_SIZE];
                for (int i = 0; i < FEATURE_SIZE; i++)
                    testInput[i] = 0f;
                float[] scaledTest = new float[FEATURE_SIZE];
                for (int i = 0; i < FEATURE_SIZE; i++)
                    scaledTest[i] = (testInput[i] - scalerMean[i]) / scalerScale[i];

                NDArray input = manager.create(scaledTest, new Shape(1, FEATURE_SIZE));
                input.setName("float_input");
                NDList inputList = new NDList(input);

                System.out.println("Testing model with scaled float_input NDArray...");
                NDList out = predictor.predict(inputList);
                System.out.println("Output shape: " + out.singletonOrThrow().getShape());
            }
            return null;
        } catch (UnsupportedOperationException uoe) {
            uoe.printStackTrace();
            return "Error: The DJL engine in use does not support this NDArray operation (reshape). " +
                   "Check that you are using the OnnxRuntime engine and that your model and input data are compatible. " +
                   "Technical error: " + uoe.getClass().getSimpleName() + " - " + uoe.getMessage();
        } catch (Exception e) {
            e.printStackTrace();
            return "Error loading model:\n\nFile: " + modelFile.getAbsolutePath() + "\n\n"
                    + "Possible causes:\n"
                    + "1. Invalid model format\n"
                    + "2. Input shape mismatch (should be a single float NDArray of shape [1, FEATURE_SIZE] named float_input)\n"
                    + "3. Missing DJL OnnxRuntime dependency\n"
                    + "4. ONNX Runtime native library not found (DLL/SO)\n"
                    + "5. Model exported incorrectly from Python\n"
                    + "6. Missing or invalid scaler_mean.csv/scaler_scale.csv\n\n"
                    + "Technical error: " + e.getClass().getSimpleName() + " - " + e.getMessage();
        }
    }

    public boolean isLoaded() {
        return predictor != null && model != null && scalerMean != null && scalerScale != null;
    }

    public String predict(List<Float> features) throws TranslateException {
        if (!isLoaded())
            throw new IllegalStateException("Predictor or scaler not initialized.");

        if (features == null || features.size() != FEATURE_SIZE) {
            throw new IllegalArgumentException("Feature vector must be of size " + FEATURE_SIZE);
        }
        for (Float f : features) {
            if (f == null || !Float.isFinite(f)) {
                return "NaN";
            }
        }

        // === DEBUG: Print the raw feature vector (after one-hot, before scaling) ===
        System.out.println("JAVA RAW FEATURES: " + features);

        // Scale features before prediction
        float[] scaled = StandardScalerUtil.scale(features, scalerMean, scalerScale);

        // === DEBUG: Print the scaled feature vector ===
        System.out.print("JAVA SCALED FEATURES: [");
        for (float f : scaled) System.out.print(f + ", ");
        System.out.println("]");

        try (NDManager manager = NDManager.newBaseManager()) {
            NDArray input = manager.create(scaled, new Shape(1, FEATURE_SIZE));
            input.setName("float_input");
            NDList inputList = new NDList(input);

            NDList output = predictor.predict(inputList);
            NDArray resultArray = output.singletonOrThrow();
            float predictedRainfall;
            if (resultArray.size() >= 1) {
                predictedRainfall = resultArray.toFloatArray()[0];
            } else {
                predictedRainfall = Float.NaN;
            }

            // === DEBUG: Print the prediction ===
            System.out.println("JAVA PREDICTION: " + predictedRainfall);

            predictionHistory.add(predictedRainfall); // Store for graph
            return String.format("%.2f mm", predictedRainfall);
        }
    }

    public void close() {
        if (predictor != null) {
            try {
                predictor.close();
            } catch (Exception ignored) {}
            predictor = null;
        }
        if (model != null) {
            try {
                model.close();
            } catch (Exception ignored) {}
            model = null;
        }
        predictionHistory.clear();
        scalerMean = null;
        scalerScale = null;
    }
}