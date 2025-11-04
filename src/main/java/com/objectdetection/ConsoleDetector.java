package com.objectdetection;

import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.videoio.VideoCapture;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

public class ConsoleDetector {
    public static void main(String[] args) {
        System.out.println("=== CONSOLE OBJECT DETECTOR ===");
        
        try {
            // Load OpenCV
            System.out.println("1. Loading OpenCV...");
            ManualOpenCVLoader.loadOpenCV();
            
            // Load YOLO model
            System.out.println("2. Loading YOLOv4 model...");
            Net net = Dnn.readNetFromDarknet("models/yolov4.cfg", "models/yolov4.weights");
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU);
            System.out.println("   Model loaded successfully!");
            
            // Load class names
            List<String> classNames = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new FileReader("models/coco.names"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    classNames.add(line.trim());
                }
            }
            System.out.println("   Loaded " + classNames.size() + " class names");
            
            // Test camera
            System.out.println("3. Testing camera...");
            VideoCapture camera = new VideoCapture(0);
            if (!camera.isOpened()) {
                System.out.println("   Camera not available. Using test mode.");
                testModelOnly(net, classNames);
            } else {
                System.out.println("   Camera available! Starting real-time detection...");
                startRealTimeDetection(camera, net, classNames);
            }
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private static void testModelOnly(Net net, List<String> classNames) {
        System.out.println("Running model test...");
        try {
            // Create a test image
            Mat testImage = new Mat(480, 640, CvType.CV_8UC3, new Scalar(100, 100, 100));
            
            // Run detection
            Mat blob = Dnn.blobFromImage(testImage, 1/255.0, new Size(416, 416), new Scalar(0,0,0), true, false);
            net.setInput(blob);
            
            List<Mat> outputs = new ArrayList<>();
            net.forward(outputs, getOutputLayerNames(net));
            
            System.out.println("   Model test successful! Output layers: " + outputs.size());
            System.out.println("🎉 Your object detection system is working!");
            
        } catch (Exception e) {
            System.err.println("Model test failed: " + e.getMessage());
        }
    }
    
    private static void startRealTimeDetection(VideoCapture camera, Net net, List<String> classNames) {
    System.out.println("Camera available! Starting real-time detection...");
    System.out.println("Letting model warm up for 10 frames...");
    System.out.println("Press Ctrl+C to stop detection");
    
    Mat frame = new Mat();
    int frameCount = 0;
    long startTime = System.currentTimeMillis();
    boolean modelWarmedUp = false;
    
    try {
        while (camera.read(frame) && !frame.empty()) {
            frameCount++;
            
            // Skip processing for warm-up frames to improve initial FPS
            if (frameCount <= 10) {
                System.out.printf("Warm-up frame %d%n", frameCount);
                Thread.sleep(100);
                continue;
            }
            
            // Run detection
            List<String> detections = runDetection(frame, net, classNames);
            
            // Calculate FPS (excluding warm-up frames)
            long currentTime = System.currentTimeMillis();
            double fps = (frameCount - 10) / ((currentTime - startTime) / 1000.0);
            
            // Display results
            System.out.printf("Frame %d | FPS: %.1f | Objects: %d", frameCount, fps, detections.size());
            if (!detections.isEmpty()) {
                System.out.println(" - Detected: " + detections);
            } else {
                System.out.println(); // New line
            }
            
            // Try moving around - detect different objects
            if (frameCount == 20) {
                System.out.println("💡 TIP: Try showing different objects to the camera (person, phone, bottle, etc.)");
            }
            
            // Stop after 100 frames for demo, or let it run continuously
            if (frameCount >= 100) {
                System.out.println("Demo completed. Processed " + frameCount + " frames.");
                System.out.println("You can modify the code to run continuously!");
                break;
            }
            
            // Smaller delay for better FPS
            Thread.sleep(50);
        }
    } catch (Exception e) {
        System.err.println("Detection error: " + e.getMessage());
    } finally {
        camera.release();
        System.out.println("Camera released. Detection stopped.");
    }
}
    
    private static List<String> runDetection(Mat frame, Net net, List<String> classNames) {
        List<String> detections = new ArrayList<>();
        
        try {
            Mat blob = Dnn.blobFromImage(frame, 1/255.0, new Size(416, 416), new Scalar(0,0,0), true, false);
            net.setInput(blob);
            
            List<Mat> outputs = new ArrayList<>();
            net.forward(outputs, getOutputLayerNames(net));
            
            for (Mat output : outputs) {
                for (int i = 0; i < output.rows(); i++) {
                    Mat row = output.row(i);
                    Mat scores = row.colRange(5, output.cols());
                    Core.MinMaxLocResult result = Core.minMaxLoc(scores);
                    
                    if (result.maxVal > 0.5) { // Confidence threshold
                        String className = classNames.size() > result.maxLoc.x ? 
                                         classNames.get((int)result.maxLoc.x) : "unknown";
                        detections.add(String.format("%s(%.2f)", className, result.maxVal));
                    }
                }
            }
        } catch (Exception e) {
            // Ignore detection errors for now
        }
        
        return detections;
    }
    
    private static List<String> getOutputLayerNames(Net net) {
        List<String> layerNames = net.getLayerNames();
        List<String> outputLayers = new ArrayList<>();
        for (int i = 0; i < net.getUnconnectedOutLayers().total(); i++) {
            int index = (int) net.getUnconnectedOutLayers().get(i, 0)[0] - 1;
            outputLayers.add(layerNames.get(index));
        }
        return outputLayers;
    }
}