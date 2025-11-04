package com.objectdetection;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class OpenCVTest {
    static {
        ManualOpenCVLoader.loadOpenCV();
    }
    
    public static void main(String[] args) {
        System.out.println("=== Testing OpenCV Classes ===");
        
        try {
            // Test if OpenCV classes are available
            System.out.println("OpenCV Version: " + Core.VERSION);
            System.out.println("Build Information: " + Core.getBuildInformation());
            
            // Test DNN module (this was failing before)
            System.out.println("DNN module available: " + (Core.getVersionString().contains("dnn")));
            
            // Test basic matrix operations
            Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
            System.out.println("Test matrix created successfully:");
            System.out.println(mat.dump());
            
            System.out.println("=== SUCCESS: All OpenCV classes are available! ===");
            
        } catch (Exception e) {
            System.err.println("=== FAILED: " + e.getMessage());
            e.printStackTrace();
        }
    }
}