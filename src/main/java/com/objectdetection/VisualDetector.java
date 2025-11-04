package com.objectdetection;

import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.videoio.VideoCapture;
import org.opencv.imgcodecs.Imgcodecs;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;

public class VisualDetector extends Application {
    
    private Net net;
    private List<String> classNames;
    private VideoCapture capture;
    private boolean isRunning = false;
    private ImageView imageView;
    private Label statusLabel;
    private Label fpsLabel;
    private Label objectsLabel;
    private int frameCount = 0;
    private long startTime = System.currentTimeMillis();
    private static final double CONFIDENCE_THRESHOLD = 0.5;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Initialize OpenCV
            System.out.println("Loading OpenCV...");
            ManualOpenCVLoader.loadOpenCV();
            
            // Initialize YOLO model
            initializeModel();
            
            // Create UI
            BorderPane root = new BorderPane();
            Scene scene = new Scene(root, 1200, 800);
            
            // Create image view for video feed
            imageView = new ImageView();
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);
            imageView.setStyle("-fx-border-color: green; -fx-border-width: 2px;");
            
            // Create status labels
            statusLabel = new Label("Ready to start detection");
            statusLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            
            fpsLabel = new Label("FPS: 0.0");
            fpsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: green;");
            
            objectsLabel = new Label("Objects: 0");
            objectsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: blue;");
            
            // Create control buttons
            Button startBtn = new Button("Start Detection");
            Button stopBtn = new Button("Stop Detection");
            
            startBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 14px;");
            stopBtn.setStyle("-fx-background-color: #f44336; -fx-text-fill: white; -fx-font-size: 14px;");
            
            startBtn.setOnAction(e -> startDetection());
            stopBtn.setOnAction(e -> stopDetection());
            
            // Create controls panel
            VBox controls = new VBox(10);
            controls.setStyle("-fx-padding: 20px; -fx-spacing: 10px;");
            controls.getChildren().addAll(startBtn, stopBtn, statusLabel, fpsLabel, objectsLabel);
            
            root.setCenter(imageView);
            root.setRight(controls);
            
            primaryStage.setTitle("Real-Time Object Detection - Live Video with Bounding Boxes");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to initialize: " + e.getMessage());
        }
    }
    
    private void initializeModel() {
        try {
            System.out.println("Loading YOLOv4 model...");
            net = Dnn.readNetFromDarknet("models/yolov4.cfg", "models/yolov4.weights");
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU);
            System.out.println("Model loaded successfully!");
            
            // Load class names
            classNames = new ArrayList<>();
            BufferedReader reader = new BufferedReader(new FileReader("models/coco.names"));
            String line;
            while ((line = reader.readLine()) != null) {
                classNames.add(line.trim());
            }
            reader.close();
            System.out.println("Loaded " + classNames.size() + " class names");
            
            statusLabel.setText("Model loaded! Click Start Detection");
            
        } catch (Exception e) {
            System.err.println("Error loading model: " + e.getMessage());
            throw new RuntimeException("Failed to load object detection model", e);
        }
    }
    
    private void startDetection() {
        if (isRunning) return;
        
        System.out.println("Opening camera...");
        capture = new VideoCapture(0);
        
        if (!capture.isOpened()) {
            showError("Cannot open camera");
            return;
        }
        
        isRunning = true;
        frameCount = 0;
        startTime = System.currentTimeMillis();
        statusLabel.setText("Detection Running - Showing bounding boxes");
        
        // Start detection thread
        Thread detectionThread = new Thread(() -> {
            Mat frame = new Mat();
            
            while (isRunning) {
                if (capture.read(frame) && !frame.empty()) {
                    try {
                        // Detect objects and draw bounding boxes
                        Mat processedFrame = detectAndDrawObjects(frame);
                        
                        // Convert to JavaFX Image
                        Image fxImage = matToJavaFXImage(processedFrame);
                        
                        // Update UI
                        if (fxImage != null) {
                            Platform.runLater(() -> {
                                imageView.setImage(fxImage);
                                
                                // Update FPS
                                long currentTime = System.currentTimeMillis();
                                double fps = frameCount / ((currentTime - startTime) / 1000.0);
                                fpsLabel.setText(String.format("FPS: %.1f", fps));
                            });
                        }
                        
                        frameCount++;
                        
                    } catch (Exception e) {
                        System.err.println("Frame processing error: " + e.getMessage());
                    }
                }
                
                try {
                    Thread.sleep(30); // ~33 FPS
                } catch (InterruptedException e) {
                    break;
                }
            }
            
            if (capture != null) {
                capture.release();
            }
        });
        
        detectionThread.setDaemon(true);
        detectionThread.start();
    }
    
    private Mat detectAndDrawObjects(Mat frame) {
        Mat resultFrame = frame.clone();
        
        // Run detection
        Mat blob = Dnn.blobFromImage(frame, 1/255.0, new Size(416, 416), new Scalar(0,0,0), true, false);
        net.setInput(blob);
        
        List<Mat> outputs = new ArrayList<>();
        net.forward(outputs, getOutputLayerNames());
        
        List<Rect> boxes = new ArrayList<>();
        List<Float> confidences = new ArrayList<>();
        List<Integer> classIds = new ArrayList<>();
        
        // Process detections
        for (Mat output : outputs) {
            for (int i = 0; i < output.rows(); i++) {
                Mat row = output.row(i);
                Mat scores = row.colRange(5, output.cols());
                Core.MinMaxLocResult result = Core.minMaxLoc(scores);
                
                if (result.maxVal > CONFIDENCE_THRESHOLD) {
                    double centerX = row.get(0, 0)[0] * frame.cols();
                    double centerY = row.get(0, 1)[0] * frame.rows();
                    double width = row.get(0, 2)[0] * frame.cols();
                    double height = row.get(0, 3)[0] * frame.rows();
                    
                    int x = (int)(centerX - width / 2);
                    int y = (int)(centerY - height / 2);
                    
                    boxes.add(new Rect(x, y, (int)width, (int)height));
                    confidences.add((float)result.maxVal);
                    classIds.add((int)result.maxLoc.x);
                }
            }
        }
        
        // Draw bounding boxes
        int objectsDetected = 0;
        for (int i = 0; i < boxes.size(); i++) {
            Rect box = boxes.get(i);
            String className = classNames.size() > classIds.get(i) ? classNames.get(classIds.get(i)) : "unknown";
            float confidence = confidences.get(i);
            
            // Draw green bounding box
            Imgproc.rectangle(resultFrame, box, new Scalar(0, 255, 0), 2);
            
            // Draw label
            String label = String.format("%s: %.2f", className, confidence);
            Imgproc.putText(resultFrame, label, new Point(box.x, box.y - 5), 
                          Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, new Scalar(0, 255, 0), 1);
            
            objectsDetected++;
        }
        
        // Update object count
        final int finalCount = objectsDetected;
        Platform.runLater(() -> {
            objectsLabel.setText("Objects: " + finalCount);
        });
        
        return resultFrame;
    }
    
    private List<String> getOutputLayerNames() {
        List<String> layerNames = net.getLayerNames();
        List<String> outputLayers = new ArrayList<>();
        for (int i = 0; i < net.getUnconnectedOutLayers().total(); i++) {
            int index = (int) net.getUnconnectedOutLayers().get(i, 0)[0] - 1;
            outputLayers.add(layerNames.get(index));
        }
        return outputLayers;
    }
    
    private Image matToJavaFXImage(Mat mat) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", mat, buffer);
            byte[] byteArray = buffer.toArray();
            return new Image(new ByteArrayInputStream(byteArray));
        } catch (Exception e) {
            System.err.println("Error converting image: " + e.getMessage());
            return null;
        }
    }
    
    private void stopDetection() {
        isRunning = false;
        if (capture != null) {
            capture.release();
        }
        statusLabel.setText("Detection Stopped");
    }
    
    private void showError(String message) {
        statusLabel.setText("ERROR: " + message);
    }
    
    @Override
    public void stop() {
        stopDetection();
    }
    
    public static void main(String[] args) {
        launch(args);
    }
}