# rainfallPredictionONNX
Combination of JavaFX application with ONNX model compatibility for training/testing LSTM functionalities in time-series forecasting capabilities, in this instance rainfall precipitation between designated time periods.
# Rainfall Prediction App

A JavaFX desktop application to run rainfall predictions using a pre-trained model (ONNX / DJL). The UI lets you load a model file, load CSV data, run predictions on each row, view/save prediction results, and show comparison/metrics graphs.

Main entry point: `rainfallPrediction.RainfallPredictionApp` (file: `RainfallPredictionApp.java`).

---

## Key features

- Load an ONNX or DJL model from disk.
- Load input CSV data and build the feature vector expected by the model.
- One-hot encoding for Malaysian state names in the ONNX order:
  - Johor, Kedah, Kelantan, Melaka, Pahang, Perak, Perlis, Pulau Pinang, Sabah, Sarawak, Selangor, Terengganu, Wilayah Persekutuan Labuan
- Run predictions for every row and show results in a table.
- Save results to CSV.
- View Actual vs Predicted charts and compute regression metrics (RMSE, MAE, MAPE, median absolute error, explained variance).
- Manual test mode method available for quick validation (`runManualTestPrediction()`).

---

## Requirements

- Java 11+ (Java 17 recommended)
- JavaFX SDK (matching your JDK version) OR use an SDK-distributed Java that bundles JavaFX.
- ONNX Runtime Java bindings or DJL (if your model uses DJL). The code comments and file chooser mention ONNX and DJL — ensure you include the correct runtime library:
  - ONNX Runtime Java: com.microsoft.onnxruntime:onnxruntime
  - Or DJL with appropriate engine (e.g., MXNet, PyTorch) if you prefer.
- A pre-trained model file (e.g., `msiarainfallmodel.onnx`).
- Optional: build tool (Maven or Gradle) to manage JavaFX and ONNX/DJL dependencies more easily.

---

## CSV input format

The loader expects a CSV containing at least the following column names (case-insensitive match performed in code):

- height
- minMeanTemp
- maxMeanTemp
- meanRelHum
- state

Optional column (for metrics / actual vs predicted graphs):
- rainfall (used as the "Actual" target value)

The app will construct a feature vector for each row in the following order:
- numeric features: `height`, `minMeanTemp`, `maxMeanTemp`, `meanRelHum`
- 13 one-hot state columns in this ONNX order:
  - `state_Johor`, `state_Kedah`, `state_Kelantan`, `state_Melaka`, `state_Pahang`, `state_Perak`, `state_Perlis`, `state_Pulau Pinang`, `state_Sabah`, `state_Sarawak`, `state_Selangor`, `state_Terengganu`, `state_Wilayah Persekutuan Labuan`

Example CSV (header + one example row):
```csv
height,minMeanTemp,maxMeanTemp,meanRelHum,state,rainfall
37.8,22.9,32.3,86.1,Johor,12.4
```

---

## Build & Run

Two approaches: using a build tool (recommended) or manual compile/run.

### Recommended: Maven (example snippets)
Create a `pom.xml` that includes JavaFX and ONNX/DJL dependencies. Example dependencies to add:

- JavaFX (use org.openjfx/javafx-bom and javafx-controls)
- ONNX Runtime Java (com.microsoft.onnxruntime:onnxruntime) or DJL packages

You can then use the JavaFX Maven plugin or run with:
```bash
mvn clean package
mvn javafx:run
```
(Exact plugin configuration depends on your chosen JavaFX and ONNX/DJL setup.)

### Manual (javac + java)

1. Make sure JavaFX SDK is downloaded and note the path to its `lib` folder (example: /path/to/javafx-sdk-17/lib).
2. Compile:
On macOS/Linux:
```bash
javac -cp "/path/to/javafx-sdk/lib/*:." rainfallPrediction/*.java
```
On Windows (PowerShell/CMD):
```cmd
javac -cp "C:\path\to\javafx-sdk\lib\*;." rainfallPrediction\*.java
```

3. Run:
On macOS/Linux:
```bash
java --module-path /path/to/javafx-sdk/lib --add-modules javafx.controls,javafx.fxml -cp . rainfallPrediction.RainfallPredictionApp
```
On Windows:
```cmd
java --module-path "C:\path\to\javafx-sdk\lib" --add-modules javafx.controls,javafx.fxml -cp . rainfallPrediction.RainfallPredictionApp
```

Notes:
- Add ONNX/DJL runtime jars to the classpath or configure via your build tool.
- If using a packaged jar, include dependencies (fat/uber jar) or provide the runtime jars on the command line.

---

## How to use the application (UI flow)

1. Launch the application (see run instructions).
2. Top-left buttons:
   - Load Model: select your `.onnx` (or DJL model) file.
   - Load CSV: pick the CSV file formatted as described above.
   - Predict: enabled once both model and data are loaded — runs predictions for every row.
   - Clear Data: clear the table and predictions.
   - Show Prediction Graph: view a chart of predicted values.
   - Show Metrics Graph: view actual vs predicted and computed metrics (RMSE, MAE, MAPE, median abs. error, explained variance).
   - Save as CSV: export table with `Actual` and `Prediction` columns.
   - Compare Metrics from CSV: select a previously-saved CSV to compute metrics.

3. After predictions run, check the Prediction column in the table and optionally save results.

---

## Troubleshooting & Tips

- "Model: Not Loaded" / model errors:
  - Ensure your model file is compatible with your chosen runtime (ONNX runtime vs DJL).
  - Ensure the model input shape and feature order match the feature vector the app constructs (17 features: 4 numeric + 13 one-hot).

- JavaFX runtime errors:
  - If you see "JavaFX runtime components are missing", you must provide the JavaFX modules via `--module-path` and `--add-modules`, or use a JDK that bundles JavaFX.

- Class/compile errors related to missing symbols:
  - Ensure `PredictionMetrics` (or `PredictionEvaluator`) naming is consistent.
  - Add the missing helper classes listed earlier (`RainfallModelService`, `CsvDataLoader`, etc.) to the project.

- Large CSVs:
  - UI may become slow for very large files. Consider chunked processing or a headless batch mode.

- Model scaling:
  - `StandardScalerUtil` can load mean/scale arrays and scale features; make sure the arrays align to the feature ordering.
  - Use the script for generating LSTM models from this repo: https://github.com/blackcontractor90/LSTMmodelgenerator

---



