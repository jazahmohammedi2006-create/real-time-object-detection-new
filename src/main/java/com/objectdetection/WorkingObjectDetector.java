package com.objectdetection;

import org.opencv.core.*;
import org.opencv.dnn.Dnn;
import org.opencv.dnn.Net;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.VideoCapture;
import javafx.scene.image.Image;
import javafx.scene.image.PixelFormat;
import javafx.scene.image.WritableImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class WorkingObjectDetector {
    private Net net;
    private VideoCapture capture;
    private boolean isRunning = false;
    private AtomicInteger detectionCount = new AtomicInteger(0);
    private boolean modelLoaded = false;
    
    // Performance optimization settings
    private final int DETECTION_INTERVAL = 5; // Process every 5th frame for detection
    private final int INPUT_SIZE = 320; // Smaller input size for faster processing
    private final double CONFIDENCE_THRESHOLD = 0.6; // Higher threshold to reduce false positives
    private final double NMS_THRESHOLD = 0.4;
    
    // Frame skipping for smooth video
    private final long MIN_FRAME_TIME_MS = 33; // ~30 FPS
    
    // Complete YOLOv4 COCO class names (80 classes)
    private final String[] classNames = {
        "person", "bicycle", "car", "motorcycle", "airplane", "bus", "train", "truck", "boat", 
        "traffic light", "fire hydrant", "stop sign", "parking meter", "bench", "bird", "cat", 
        "dog", "horse", "sheep", "cow", "elephant", "bear", "zebra", "giraffe", "backpack", 
        "umbrella", "handbag", "tie", "suitcase", "frisbee", "skis", "snowboard", "sports ball", 
        "kite", "baseball bat", "baseball glove", "skateboard", "surfboard", "tennis racket", 
        "bottle", "wine glass", "cup", "fork", "knife", "spoon", "bowl", "banana", "apple", 
        "sandwich", "orange", "broccoli", "carrot", "hot dog", "pizza", "donut", "cake", "chair", 
        "couch", "potted plant", "bed", "dining table", "toilet", "tv", "laptop", "mouse", 
        "remote", "keyboard", "cell phone", "microwave", "oven", "toaster", "sink", "refrigerator", 
        "book", "clock", "vase", "scissors", "teddy bear", "hair drier", "toothbrush"
    };
    
    public WorkingObjectDetector() {
        loadModel();
    }
    
    private void loadModel() {
        try {
            System.out.println("Initializing Object Detector...");
            System.out.println("Performance Mode: Optimized for CPU");
            
            File weightsFile = new File("yolov4.weights");
            File configFile = new File("yolov4.cfg");
            
            System.out.println("Weights exists: " + weightsFile.exists());
            System.out.println("Config exists: " + configFile.exists());
            
            if (!weightsFile.exists() || !configFile.exists()) {
                System.err.println("Model files not found! Running in camera-only mode.");
                return;
            }
            
            // Load the YOLOv4 network
            System.out.println("Loading YOLOv4 model (this may take a moment)...");
            this.net = Dnn.readNetFromDarknet("yolov4.cfg", "yolov4.weights");
            net.setPreferableBackend(Dnn.DNN_BACKEND_OPENCV);
            net.setPreferableTarget(Dnn.DNN_TARGET_CPU);
            
            System.out.println("✓ YOLOv4 model loaded successfully!");
            System.out.println("✓ Detection interval: Every " + DETECTION_INTERVAL + " frames");
            System.out.println("✓ Input size: " + INPUT_SIZE + "x" + INPUT_SIZE);
            System.out.println("✓ Confidence threshold: " + CONFIDENCE_THRESHOLD);
            System.out.println("✓ Classes available: " + classNames.length);
            modelLoaded = true;
            
        } catch (Exception e) {
            System.err.println("Error loading model: " + e.getMessage());
        }
    }
    
    public void startRealTimeDetection(java.util.function.Consumer<Image> imageConsumer) {
        try {
            capture = new VideoCapture(0);
            if (!capture.isOpened()) {
                System.err.println("Cannot open camera!");
                return;
            }
            
            isRunning = true;
            System.out.println("Starting optimized real-time detection...");
            
            // Single thread for both camera reading and processing
            new Thread(() -> {
                Mat frame = new Mat();
                int frameCounter = 0;
                List<Detection> lastDetections = new ArrayList<>();
                
                while (isRunning) {
                    long frameStartTime = System.currentTimeMillis();
                    
                    // Read frame from camera
                    boolean frameRead = capture.read(frame);
                    if (!frameRead || frame.empty()) {
                        System.err.println("Failed to read frame from camera");
                        try { Thread.sleep(100); } catch (InterruptedException e) {}
                        continue;
                    }
                    
                    try {
                        Mat processedFrame;
                        List<Detection> currentDetections = lastDetections;
                        
                        // Run detection only on specified interval frames
                        if (modelLoaded && frameCounter % DETECTION_INTERVAL == 0) {
                            currentDetections = detectObjects(frame);
                            lastDetections = currentDetections;
                            detectionCount.set(currentDetections.size());
                            
                            processedFrame = drawDetectionsOnFrame(frame.clone(), currentDetections);
                        } else {
                            // Use last detection results for non-detection frames
                            processedFrame = drawDetectionsOnFrame(frame.clone(), lastDetections);
                        }
                        
                        // Convert to JavaFX image and send to UI
                        Image fxImage = mat2Image(processedFrame);
                        if (fxImage != null) {
                            javafx.application.Platform.runLater(() -> {
                                imageConsumer.accept(fxImage);
                            });
                        }
                        
                        frameCounter++;
                        
                        // Frame rate control
                        long processingTime = System.currentTimeMillis() - frameStartTime;
                        long sleepTime = Math.max(1, MIN_FRAME_TIME_MS - processingTime);
                        if (sleepTime > 0) {
                            Thread.sleep(sleepTime);
                        }
                        
                    } catch (Exception e) {
                        System.err.println("Error processing frame: " + e.getMessage());
                    }
                }
                
                if (capture != null) {
                    capture.release();
                    System.out.println("Camera released");
                }
            }).start();
            
        } catch (Exception e) {
            System.err.println("Error starting detection: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private List<Detection> detectObjects(Mat frame) {
        List<Detection> detections = new ArrayList<>();
        
        try {
            // Create blob with smaller size for faster processing
            Mat blob = Dnn.blobFromImage(frame, 1/255.0, new Size(INPUT_SIZE, INPUT_SIZE), 
                                       new Scalar(0, 0, 0), true, false);
            net.setInput(blob);
            
            // Run forward pass
            List<Mat> outputs = new ArrayList<>();
            net.forward(outputs, getOutputsNames());
            
            // Process outputs with higher confidence threshold
            for (Mat output : outputs) {
                for (int i = 0; i < output.rows(); i++) {
                    Mat row = output.row(i);
                    Mat scores = row.colRange(5, output.cols());
                    Core.MinMaxLocResult result = Core.minMaxLoc(scores);
                    
                    double confidence = result.maxVal;
                    int classId = (int) result.maxLoc.x;
                    
                    if (confidence > CONFIDENCE_THRESHOLD && classId >= 0 && classId < classNames.length) {
                        double centerX = row.get(0, 0)[0] * frame.cols();
                        double centerY = row.get(0, 1)[0] * frame.rows();
                        double width = row.get(0, 2)[0] * frame.cols();
                        double height = row.get(0, 3)[0] * frame.rows();
                        
                        double x = Math.max(0, centerX - width / 2);
                        double y = Math.max(0, centerY - height / 2);
                        width = Math.min(width, frame.cols() - x);
                        height = Math.min(height, frame.rows() - y);
                        
                        if (width > 10 && height > 10) { // Filter very small detections
                            Rect rect = new Rect((int)x, (int)y, (int)width, (int)height);
                            detections.add(new Detection(classId, confidence, rect));
                        }
                    }
                }
            }
            
            // Apply NMS to remove duplicates
            detections = applyNMS(detections);
            
        } catch (Exception e) {
            System.err.println("Detection error: " + e.getMessage());
        }
        
        return detections;
    }
    
    private Mat drawDetectionsOnFrame(Mat frame, List<Detection> detections) {
        // Draw all detections
        for (Detection detection : detections) {
            drawDetection(frame, detection);
        }
        
        // Add performance info
        addPerformanceInfo(frame, detections.size());
        
        return frame;
    }
    
    private List<Detection> applyNMS(List<Detection> detections) {
        List<Detection> result = new ArrayList<>();
        boolean[] suppressed = new boolean[detections.size()];
        
        detections.sort((d1, d2) -> Double.compare(d2.confidence, d1.confidence));
        
        for (int i = 0; i < detections.size(); i++) {
            if (suppressed[i]) continue;
            
            Detection current = detections.get(i);
            result.add(current);
            
            for (int j = i + 1; j < detections.size(); j++) {
                if (suppressed[j]) continue;
                
                Detection other = detections.get(j);
                double iou = calculateIOU(current.bbox, other.bbox);
                
                if (iou > NMS_THRESHOLD) {
                    suppressed[j] = true;
                }
            }
        }
        
        return result;
    }
    
    private double calculateIOU(Rect rect1, Rect rect2) {
        int x1 = Math.max(rect1.x, rect2.x);
        int y1 = Math.max(rect1.y, rect2.y);
        int x2 = Math.min(rect1.x + rect1.width, rect2.x + rect2.width);
        int y2 = Math.min(rect1.y + rect1.height, rect2.y + rect2.height);
        
        int intersectionWidth = Math.max(0, x2 - x1);
        int intersectionHeight = Math.max(0, y2 - y1);
        int intersectionArea = intersectionWidth * intersectionHeight;
        
        int area1 = rect1.width * rect1.height;
        int area2 = rect2.width * rect2.height;
        int unionArea = area1 + area2 - intersectionArea;
        
        return unionArea > 0 ? (double) intersectionArea / unionArea : 0.0;
    }
    
    private void drawDetection(Mat frame, Detection detection) {
        Scalar color = getColorForClass(detection.classId);
        
        // Draw bounding box
        Imgproc.rectangle(frame, detection.bbox, color, 2);
        
        // Draw label with simplified formatting for performance
        String label = classNames[detection.classId] + ":" + String.format("%.1f", detection.confidence);
        
        Point textPoint = new Point(detection.bbox.x, detection.bbox.y - 5);
        Imgproc.putText(frame, label, textPoint, Imgproc.FONT_HERSHEY_SIMPLEX, 0.5, color, 2);
    }
    
    private Scalar getColorForClass(int classId) {
        Scalar[] colors = {
            new Scalar(0, 255, 0),    // Green - person
            new Scalar(255, 0, 0),    // Blue - vehicle
            new Scalar(0, 0, 255),    // Red - animal
            new Scalar(255, 255, 0),  // Cyan - electronic
            new Scalar(255, 0, 255),  // Magenta - food
            new Scalar(0, 255, 255),  // Yellow - furniture
            new Scalar(128, 0, 128),  // Purple - accessory
            new Scalar(255, 165, 0)   // Orange - other
        };
        return colors[classId % colors.length];
    }
    
    private void addPerformanceInfo(Mat frame, int objectCount) {
        // Status information
        String status = modelLoaded ? 
            "Detected: " + objectCount + " objects" : 
            "Camera Only Mode";
        
        Scalar color = modelLoaded ? new Scalar(0, 255, 0) : new Scalar(255, 255, 0);
        
        Imgproc.putText(frame, status, new Point(10, 25), 
                       Imgproc.FONT_HERSHEY_SIMPLEX, 0.6, color, 2);
        
        // Performance info
        if (modelLoaded) {
            Imgproc.putText(frame, "YOLOv4 (Optimized)", new Point(10, 50), 
                           Imgproc.FONT_HERSHEY_SIMPLEX, 0.4, new Scalar(200, 200, 200), 1);
        }
        
        // Instructions
        Imgproc.putText(frame, "Press 'Capture' to save", new Point(10, 70), 
                       Imgproc.FONT_HERSHEY_SIMPLEX, 0.4, new Scalar(255, 255, 255), 1);
    }
    
    private List<String> getOutputsNames() {
        List<String> names = new ArrayList<>();
        List<Integer> outLayers = net.getUnconnectedOutLayers().toList();
        List<String> layersNames = net.getLayerNames();
        
        for (Integer index : outLayers) {
            names.add(layersNames.get(index - 1));
        }
        return names;
    }
    
    private Image mat2Image(Mat frame) {
        try {
            if (frame.empty()) return null;
            
            Mat rgbFrame = new Mat();
            Imgproc.cvtColor(frame, rgbFrame, Imgproc.COLOR_BGR2RGB);
            
            byte[] buffer = new byte[rgbFrame.cols() * rgbFrame.rows() * (int)rgbFrame.elemSize()];
            rgbFrame.get(0, 0, buffer);
            
            WritableImage writableImage = new WritableImage(rgbFrame.cols(), rgbFrame.rows());
            writableImage.getPixelWriter().setPixels(0, 0, rgbFrame.cols(), rgbFrame.rows(),
                    PixelFormat.getByteRgbInstance(), buffer, 0, rgbFrame.cols() * 3);
            
            return writableImage;
        } catch (Exception e) {
            return null;
        }
    }
    
    public void stopDetection() {
        isRunning = false;
        if (capture != null) {
            capture.release();
        }
        System.out.println("Detection stopped");
    }
    
    public void captureCurrentFrame() {
        try {
            Mat frame = new Mat();
            if (capture != null && capture.read(frame) && !frame.empty()) {
                String filename = "capture_" + System.currentTimeMillis() + ".jpg";
                Imgcodecs.imwrite(filename, frame);
                System.out.println("✓ Captured: " + filename + " (" + detectionCount.get() + " objects)");
            }
        } catch (Exception e) {
            System.err.println("Capture error: " + e.getMessage());
        }
    }
    
    public int getLastDetectionCount() {
        return detectionCount.get();
    }
    
    private static class Detection {
        int classId;
        double confidence;
        Rect bbox;
        
        Detection(int classId, double confidence, Rect bbox) {
            this.classId = classId;
            this.confidence = confidence;
            this.bbox = bbox;
        }
    }
}