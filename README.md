# GluCare 🩺

[![Kotlin](https://img.shields.io/badge/Kotlin-1.9.0-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Android Target SDK](https://img.shields.io/badge/TargetSDK-36-green.svg?style=flat&logo=android)](https://developer.android.com)
[![Firebase](https://img.shields.io/badge/Firebase-Firestore%20%26%20Auth-orange.svg?style=flat&logo=firebase)](https://firebase.google.com)
[![TensorFlow Lite](https://img.shields.io/badge/TensorFlow-Lite%20(ResNet50)-orange.svg?style=flat&logo=tensorflow)](https://www.tensorflow.org/lite)

**GluCare** is a Kotlin-based Android application designed to help diabetic patients (and general users) monitor their health independently. The app integrates **Machine Learning** to classify wound types (specifically for early detection of high-risk diabetic wounds) and offers a **blood sugar and food intake logging system** with interactive charts.

---

## 🌟 Core Features

### 1. AI Wound Classification
* **TensorFlow Lite (TFLite)**: Powered by a custom **ResNet50-based** image classification model (input size `299x299` with manual BGR preprocessing and Mean Subtraction).
* **10 Detection Categories**:
  1. *Abrasions*
  2. *Bruises*
  3. *Burns*
  4. *Cut*
  5. *Diabetic Wounds* (High Risk)
  6. *Laceration*
  7. *Normal* (Healthy Skin)
  8. *Pressure Wounds*
  9. *Surgical Wounds*
  10. *Venous Wounds*
* **Confidence Level & Smart Actions**: Displays the prediction's confidence score. If the AI detects **Diabetic Wounds** or other severe wound types, a **"MEDICAL ATTENTION REQUIRED"** alert is triggered, prompting the user to locate the nearest medical facility.

### 2. Google Maps Integration (Hospital Finder)
* Integrated with smartphone location services (`LocationActivity`).
* If a critical wound is identified, users can quickly search for nearby **"Hospitals"** on Google Maps with a single click.

### 3. Food & Blood Sugar Logs
* Track consumed meals alongside blood sugar measurements (mg/dL).
* Custom date and time picker to map entries accurately on the timeline.
* Full support for adding, editing, and deleting food log history.

### 4. Interactive Charts & Statistics (MPAndroidChart)
* **Bar Chart (Home Screen)**: Summarizes frequency stats of the user's scanned wound types.
* **Line Chart (Logs Screen)**: Visualizes blood sugar trends over time using a *Cubic Bezier* line plot with a *Custom MarkerView* displaying tooltips for precise data points.
* **Time Filters**: Filter blood sugar history by Day, Week, or Month.

### 5. Firebase Cloud Backend
* **Firebase Authentication**: Secure registration, login, and user session management.
* **Cloud Firestore**: Real-time cloud database synchronization for wound analysis history (`wound_history`) and food logs (`food_logs`).
* **Offline Image Recovery**: Encodes captured wound images to Base64 and backs them up to Cloud Firestore. This ensures history logs remain viewable even if the local app cache is cleared.

### 6. Account & Profile Management
* Customizable username and phone number settings.
* Built-in FAQ (Frequently Asked Questions) and Privacy Policy activities.
* **Delete Account Option**: Completely wipes user profile details, food logs, and wound history from both Cloud Firestore and Firebase Auth for data privacy.

---

## 🛠️ Tech Stack & Dependencies

* **Language**: [Kotlin](https://kotlinlang.org/)
* **Platform**: Android SDK (Min SDK: 24, Target SDK: 36)
* **UI/UX**: Material Design Components (MDC), AndroidX SplashScreen API
* **Backend**:
  * Firebase Auth (Authentication)
  * Cloud Firestore (Real-time Database)
* **Machine Learning**: TensorFlow Lite (Interpreter, Support Library, GPU Delegate)
* **Camera**: CameraX (Core, Camera2, Lifecycle, View)
* **Data Visualization**: [MPAndroidChart](https://github.com/PhilJay/MPAndroidChart)

---

## 📂 Project Structure

```text
app/src/main/java/com/example/mapmidtermproject/
├── activities/            # UI Activities (Main, Log, Analysis, Login, Settings, etc.)
├── adapters/              # RecyclerView Adapters (WoundHistory, FoodLog, LocalImage)
├── models/                # Data Models (UserDoc, WoundAnalysis)
├── repositories/          # Data flow repository classes
├── settings/              # Settings-related activities and preference helpers
└── utils/                 # Utility helpers (FirestoreHelper, WoundClassifierHelper, Event)
```

---

## 🚀 Getting Started

1. **Clone the Repository**:
   ```bash
   git clone https://github.com/stevjoo/glucare.git
   cd glucare
   ```
2. **Setup Firebase**:
   * Create a new project in the [Firebase Console](https://console.firebase.google.com/).
   * Enable **Firebase Authentication** (Email/Password provider) and **Cloud Firestore**.
   * Download the `google-services.json` config file and place it in the `app/` directory of the project.
3. **Build & Run**:
   * Open the project in **Android Studio**.
   * Wait for Gradle synchronization to finish.
   * Connect a physical Android device or set up an Emulator with camera features enabled.
   * Run the app by clicking **Run 'app'** in Android Studio.

