package com.objectdetection;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class MainApp extends Application {
    
    private WorkingObjectDetector detector;
    private ImageView imageView;
    private Label statusLabel;
    private boolean isDetecting = false;
    
    @Override
    public void start(Stage primaryStage) {
        try {
            ManualOpenCVLoader.loadOpenCV();
            detector = new WorkingObjectDetector();
            statusLabel = new Label("OpenCV and YOLOv4 model loaded successfully!");
            
            BorderPane root = new BorderPane();
            Scene scene = new Scene(root, 1200, 800);
            
            imageView = new ImageView();
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);
            
            VBox controls = new VBox(10);
            Button startBtn = new Button("Start Detection");
            Button stopBtn = new Button("Stop Detection");
            Button captureBtn = new Button("Capture Image");
            
            startBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
            stopBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
            captureBtn.setStyle("-fx-font-size: 14px; -fx-padding: 10px;");
            
            startBtn.setOnAction(e -> startDetection());
            stopBtn.setOnAction(e -> stopDetection());
            captureBtn.setOnAction(e -> captureImage());
            
            controls.getChildren().addAll(startBtn, stopBtn, captureBtn, statusLabel);
            controls.setStyle("-fx-padding: 20px; -fx-spacing: 10px;");
            
            root.setCenter(imageView);
            root.setRight(controls);
            
            primaryStage.setTitle("Real-Time Object Detection with YOLOv4");
            primaryStage.setScene(scene);
            primaryStage.show();
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Failed to initialize: " + e.getMessage());
        }
    }
    
    private void startDetection() {
    if (!isDetecting) {
        isDetecting = true;
        statusLabel.setText("Starting camera...");
        
        new Thread(() -> {
            detector.startRealTimeDetection((image) -> {
                javafx.application.Platform.runLater(() -> {
                    if (image != null) {
                        imageView.setImage(image);
                        statusLabel.setText("Camera running - " + 
                            detector.getLastDetectionCount() + " objects detected");
                    } else {
                        statusLabel.setText("ERROR: No image received from camera");
                    }
                });
            });
        }).start();
    }
}
    
    private void stopDetection() {
        isDetecting = false;
        detector.stopDetection();
        statusLabel.setText("Detection stopped");
    }
    
    private void captureImage() {
        detector.captureCurrentFrame();
        statusLabel.setText("Image captured and saved to project directory");
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