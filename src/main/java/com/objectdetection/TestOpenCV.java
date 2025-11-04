package com.objectdetection;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

public class TestOpenCV {
    public static void main(String[] args) {
        System.out.println("=== Testing OpenCV Setup ===");
        
        try {
            // Load OpenCV
            ManualOpenCVLoader.loadOpenCV();
            
            // Test basic functionality
            System.out.println("OpenCV Version: " + Core.VERSION);
            System.out.println("OpenCV Native Library: " + Core.NATIVE_LIBRARY_NAME);
            
            // Create a test matrix
            Mat mat = Mat.eye(3, 3, CvType.CV_8UC1);
            System.out.println("Test Matrix:");
            System.out.println(mat.dump());
            
            System.out.println("=== SUCCESS: OpenCV is working correctly! ===");
            
        } catch (Exception e) {
            System.err.println("=== FAILED: OpenCV test failed ===");
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
            
            System.out.println("\nTroubleshooting steps:");
            System.out.println("1. Run: .\\download-opencv.ps1");
            System.out.println("2. Check that 'lib/opencv_java490.dll' exists");
            System.out.println("3. Make sure you're running from the project root directory");
        }
    }
}