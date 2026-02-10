# How to Run RoboFace via Command Line (No Android Studio GUI)

Since this project was generated programmatically, it does not include the binary Gradle Wrapper (`gradle-wrapper.jar`). You will need to use a local installation of Gradle or the one bundled with your Android Studio installation.

## Prerequisites

1.  **JDK (Java Development Kit)**: Version 17 or higher (recommended for Android compilation).
2.  **Android SDK**: Must be installed.
3.  **Command Line Tools**: `adb` and `gradle` (or use the helper script below).

## Step 1: Set up SDK Location

Create a file named `local.properties` in this root directory (`RoboFace/`) if it doesn't exist. Add the path to your Android SDK.

**Windows Example:**
```properties
sdk.dir=C\:\\Users\\YOUR_USERNAME\\AppData\\Local\\Android\\Sdk
```
*(Note the double backslashes)*

## Step 2: Build the APK

You need to execute the `assembleDebug` Gradle task.

### Option A: You have Gradle installed globally
Open a terminal in this directory and run:

```powershell
gradle assembleDebug
```

### Option B: Using Android Studio's bundled Gradle

If you don't have gradle in your PATH, find it in your Android Studio installation path:

```powershell
"C:\Program Files\Android\Android Studio\gradle\gradle-8.x\bin\gradle" assembleDebug
```

*(Replace `8.x` with your actual version version)*

## Step 3: Install on Device

Once the build is successful, the APK will be generated at:
`./app/build/outputs/apk/debug/app-debug.apk`

Connect your phone via USB (ensure USB Debugging is ON) and run:

```powershell
# Check connection
adb devices

# Install
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Troubleshooting
*   **'adb' not found**: Add your SDK platform-tools to your PATH:
    `%LOCALAPPDATA%\Android\Sdk\platform-tools`
*   **'JAVA_HOME' not set**: Ensure your `JAVA_HOME` environment variable points to your JDK installation.
