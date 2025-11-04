package com.objectdetection;

import org.opencv.core.Mat;
import org.opencv.videoio.VideoCapture;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class CameraTest extends Application {
    private VideoCapture capture;
    private ImageView imageView;
    private boolean isRunning = false;

    static {
        ManualOpenCVLoader.loadOpenCV();
    }

    @Override
    public void start(Stage primaryStage) {
        try {
            System.out.println("Starting Camera Test...");
            
            imageView = new ImageView();
            imageView.setPreserveRatio(true);
            imageView.setFitWidth(800);
            imageView.setFitHeight(600);

            StackPane root = new StackPane();
            root.getChildren().add(imageView);
            Scene scene = new Scene(root, 800, 600);

            primaryStage.setTitle("Camera Test");
            primaryStage.setScene(scene);
            primaryStage.show();

            // Start camera
            startCamera();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startCamera() {
        try {
            System.out.println("Initializing camera...");
            
            // Try different camera indices
            for (int i = 0; i < 3; i++) {
                capture = new VideoCapture(i);
                if (capture.isOpened()) {
                    System.out.println("Camera found at index: " + i);
                    break;
                } else {
                    System.out.println("Camera not found at index: " + i);
                }
            }

            if (!capture.isOpened()) {
                System.err.println("ERROR: Cannot open any camera!");
                showErrorImage();
                return;
            }

            isRunning = true;
            System.out.println("Camera opened successfully!");

            // Start camera thread
            new Thread(() -> {
                Mat frame = new Mat();
                while (isRunning) {
                    if (capture.read(frame) && !frame.empty()) {
                        try {
                            Image image = matToImage(frame);
                            if (image != null) {
                                javafx.application.Platform.runLater(() -> {
                                    imageView.setImage(image);
                                });
                            }
                            Thread.sleep(33); // ~30 FPS
                        } catch (Exception e) {
                            System.err.println("Error processing frame: " + e.getMessage());
                        }
                    } else {
                        System.err.println("Cannot read frame from camera");
                        break;
                    }
                }
                capture.release();
            }).start();

        } catch (Exception e) {
            System.err.println("Error starting camera: " + e.getMessage());
            e.printStackTrace();
            showErrorImage();
        }
    }

    private Image matToImage(Mat frame) {
        try {
            if (frame.empty()) {
                return null;
            }

            // Convert BGR to RGB
            org.opencv.core.Mat rgbFrame = new org.opencv.core.Mat();
            org.opencv.imgproc.Imgproc.cvtColor(frame, rgbFrame, org.opencv.imgproc.Imgproc.COLOR_BGR2RGB);

            // Convert to byte array
            byte[] buffer = new byte[rgbFrame.cols() * rgbFrame.rows() * (int) rgbFrame.elemSize()];
            rgbFrame.get(0, 0, buffer);

            // Create JavaFX image
            javafx.scene.image.WritableImage writableImage = 
                new javafx.scene.image.WritableImage(rgbFrame.cols(), rgbFrame.rows());
            
            writableImage.getPixelWriter().setPixels(0, 0, rgbFrame.cols(), rgbFrame.rows(),
                    javafx.scene.image.PixelFormat.getByteRgbInstance(), buffer, 0, rgbFrame.cols() * 3);

            return writableImage;
        } catch (Exception e) {
            System.err.println("Error converting Mat to Image: " + e.getMessage());
            return null;
        }
    }

    private void showErrorImage() {
        // Create a simple error image
        javafx.scene.image.WritableImage errorImage = 
            new javafx.scene.image.WritableImage(400, 300);
        
        javafx.application.Platform.runLater(() -> {
            imageView.setImage(errorImage);
            System.out.println("Displaying error image - camera not available");
        });
    }

    @Override
    public void stop() {
        isRunning = false;
        if (capture != null) {
            capture.release();
        }
        System.out.println("Camera test stopped");
    }

    public static void main(String[] args) {
        launch(args);
    }
}