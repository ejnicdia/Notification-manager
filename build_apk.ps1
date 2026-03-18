$ErrorActionPreference = "Stop"
$ProgressPreference = "SilentlyContinue"

$SdkDir = "$env:LOCALAPPDATA\Android\Sdk"
$CmdLineToolsZip = "commandlinetools-win-10406996_latest.zip"
$CmdLineToolsUrl = "https://dl.google.com/android/repository/$CmdLineToolsZip"

if (-not (Test-Path "$SdkDir\cmdline-tools\latest\bin\sdkmanager.bat")) {
    Write-Host "Downloading Android SDK Command Line Tools..."
    New-Item -ItemType Directory -Force -Path $SdkDir | Out-Null
    Invoke-WebRequest -Uri $CmdLineToolsUrl -OutFile $CmdLineToolsZip
    
    Write-Host "Extracting..."
    Expand-Archive -Path $CmdLineToolsZip -DestinationPath "$SdkDir\cmdline-tools" -Force
    
    Rename-Item -Path "$SdkDir\cmdline-tools\cmdline-tools" -NewName "latest"
    Remove-Item $CmdLineToolsZip
}

$Env:ANDROID_HOME = $SdkDir

Write-Host "Accepting licenses..."
$yesChars = "y`n" * 100
$yesChars | & "$SdkDir\cmdline-tools\latest\bin\sdkmanager.bat" --licenses

Write-Host "Installing platforms and build-tools..."
& "$SdkDir\cmdline-tools\latest\bin\sdkmanager.bat" "platform-tools" "platforms;android-34" "build-tools;34.0.0"

if (-not (Test-Path ".\gradle\gradle-8.6\bin\gradle.bat")) {
    Write-Host "Downloading Gradle 8.6..."
    New-Item -ItemType Directory -Force -Path ".\gradle" | Out-Null
    Invoke-WebRequest -Uri "https://services.gradle.org/distributions/gradle-8.6-bin.zip" -OutFile "gradle-bin.zip"
    Expand-Archive -Path "gradle-bin.zip" -DestinationPath ".\gradle" -Force
    Remove-Item "gradle-bin.zip"
}

Write-Host "Building APK..."
& ".\gradle\gradle-8.6\bin\gradle.bat" assembleDebug

if ($LASTEXITCODE -eq 0) {
    Write-Host "Build Successful! APK is located at: app\build\outputs\apk\debug\app-debug.apk"
} else {
    Write-Host "Build Failed."
    exit 1
}
