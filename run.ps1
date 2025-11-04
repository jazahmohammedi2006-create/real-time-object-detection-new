# run.ps1
Write-Host "Starting Object Detection Application..." -ForegroundColor Green

# Check for OpenCV DLL
if (-not (Test-Path "lib\opencv_java490.dll")) {
    Write-Host "ERROR: OpenCV DLL not found in lib directory!" -ForegroundColor Red
    Write-Host "Please download OpenCV from:" -ForegroundColor Yellow
    Write-Host "https://github.com/opencv/opencv/releases/download/4.9.0/opencv-4.9.0-windows.exe" -ForegroundColor Cyan
    Write-Host "Extract and copy 'opencv\build\java\x64\opencv_java490.dll' to 'lib\'" -ForegroundColor Yellow
    exit 1
}

# Compile and run
Write-Host "Compiling project..." -ForegroundColor Yellow
mvn compile

Write-Host "Running application..." -ForegroundColor Yellow
mvn exec:java "-Dexec.mainClass=com.objectdetection.MainApp"