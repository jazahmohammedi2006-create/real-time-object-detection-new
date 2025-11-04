# download-yolov4.ps1
Write-Host "=== YOLOv4 Weights Download ===" -ForegroundColor Cyan

$outputFile = "yolov4.weights"

# Check if file already exists
if (Test-Path $outputFile) {
    $size = (Get-Item $outputFile).Length
    $sizeMB = [math]::Round($size / 1MB, 2)
    Write-Host "File already exists: $sizeMB MB" -ForegroundColor Green
    exit 0
}

Write-Host "Downloading yolov4.weights (244MB)..." -ForegroundColor Yellow
Write-Host "This may take several minutes..." -ForegroundColor Yellow

# Primary download URL
$url = "https://github.com/AlexeyAB/darknet/releases/download/darknet_yolo_v3_optimal/yolov4.weights"

try {
    # Download the file
    Invoke-WebRequest -Uri $url -OutFile $outputFile
    
    # Check if download was successful
    if (Test-Path $outputFile) {
        $size = (Get-Item $outputFile).Length
        $sizeMB = [math]::Round($size / 1MB, 2)
        
        Write-Host "Download completed!" -ForegroundColor Green
        Write-Host "File size: $sizeMB MB" -ForegroundColor Green
        Write-Host "Location: $(Get-Location)\$outputFile" -ForegroundColor Cyan
        
        if ($sizeMB -lt 200) {
            Write-Host "Warning: File seems smaller than expected." -ForegroundColor Yellow
        }
    } else {
        Write-Host "Download failed - file not created" -ForegroundColor Red
    }
} catch {
    Write-Host "Download error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host "=== Download Process Complete ===" -ForegroundColor Cyan
