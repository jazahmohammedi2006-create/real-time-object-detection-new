@echo off
echo === Real-Time Object Detection ===
echo.

echo Compiling...
javac -cp "src/main/java;lib/opencv.jar" -d target/classes src/main/java/com/objectdetection/ConsoleDetector.java

if %errorlevel% neq 0 (
    echo Compilation failed!
    pause
    exit /b 1
)

echo Starting Object Detection...
echo Press Ctrl+C to stop
echo.

java -cp "target/classes;lib/opencv.jar" -Djava.library.path="C:\Users\Jazah Mohammedi\Downloads\opencv\build\java\x64" com.objectdetection.ConsoleDetector

pause