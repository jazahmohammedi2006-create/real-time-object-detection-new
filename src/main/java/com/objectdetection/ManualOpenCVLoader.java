package com.objectdetection;

import java.io.File;

public class ManualOpenCVLoader {
    private static boolean loaded = false;
    
    public static void loadOpenCV() {
        if (!loaded) {
            try {
                System.out.println("Loading OpenCV...");
                
                // Method 1: Try to load from lib directory
                File dllFile = new File("lib/opencv_java490.dll");
                if (dllFile.exists()) {
                    System.load(dllFile.getAbsolutePath());
                    System.out.println("SUCCESS: OpenCV loaded from lib directory");
                    loaded = true;
                    return;
                }
                
                // Method 2: Try current directory
                dllFile = new File("opencv_java490.dll");
                if (dllFile.exists()) {
                    System.load(dllFile.getAbsolutePath());
                    System.out.println("SUCCESS: OpenCV loaded from current directory");
                    loaded = true;
                    return;
                }
                
                // Method 3: Try system library
                try {
                    System.loadLibrary("opencv_java490");
                    System.out.println("SUCCESS: OpenCV loaded from system library path");
                    loaded = true;
                    return;
                } catch (UnsatisfiedLinkError e) {
                    System.out.println("System library load failed: " + e.getMessage());
                }
                
                throw new RuntimeException(
                    "OpenCV native library not found.\n" +
                    "Please ensure 'opencv_java490.dll' is in the 'lib' directory.\n" +
                    "If you don't have it, download from:\n" +
                    "https://github.com/opencv/opencv/releases/download/4.9.0/opencv-4.9.0-windows.exe\n" +
                    "Extract and copy 'opencv/build/java/x64/opencv_java490.dll' to 'lib/'"
                );
                
            } catch (Exception e) {
                System.err.println("ERROR: Failed to load OpenCV");
                e.printStackTrace();
                throw new RuntimeException("OpenCV native library could not be loaded: " + e.getMessage(), e);
            }
        }
    }
}