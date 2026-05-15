$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Users\aravi\Downloads\microsoft-jdk-17.0.19-windows-x64\jdk-17.0.19+10"
$env:ANDROID_HOME = "C:\mindmatrix\.tools\android-sdk"
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:Path = "$env:JAVA_HOME\bin;$env:ANDROID_HOME\platform-tools;$env:ANDROID_HOME\cmdline-tools\latest\bin;$env:Path"

if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat assembleDebug
} else {
    & "C:\Users\aravi\Downloads\gradle-8.7-bin\gradle-8.7\bin\gradle.bat" assembleDebug
}
