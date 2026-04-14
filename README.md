# StudySphere

StudySphere is an Android planner for students who want to track classes, attendance, and assignments in one place.

## Download

Stable release (v1.0.0):
[StudySphere.apk](https://github.com/Chirag8405/StudySphere/releases/download/1.0.0/StudySphere.apk)

## Core Features

### Dashboard
- Today's lecture timeline
- Quick attendance marking (Present, Absent, Cancelled)
- Summary cards for attendance health and upcoming work
- Pull to refresh

### Subjects and Lectures
- Create, edit, and delete subjects
- Choose a color for each subject
- Set minimum attendance target per subject
- Add recurring lecture slots with day, time, and room

### Attendance Tracking
- Subject-wise attendance percentage and progress
- Per-subject history with edit support
- Attendance insights:
  - Can skip N classes
  - Must attend N classes to recover
- Risk labels: Safe, Warning, Danger, Critical

### Assignments
- Create assignments with title, notes, priority, and due date
- Date picker for due date entry
- Other category for non-subject tasks
- Search plus filters by status/priority and subject
- Mark complete or undo directly from list
- Overdue highlighting

### UI and Accessibility
- Light and dark theme support (saved preference)
- Large touch targets and high-contrast status colors
- Consistent spacing and readable typography

## Tech Stack

- Kotlin
- Jetpack Compose + Material 3
- Navigation Compose
- Room Database
- DataStore Preferences
- Kotlin Coroutines + Flow

## Requirements

- Android Studio (Hedgehog or newer)
- JDK 17
- Android SDK 34
- Minimum Android version: 8.0 (API 26)

## Run Locally

1. Open the project in Android Studio.
2. Sync Gradle.
3. Run the app module on an emulator or device (API 26+).

Or from terminal:

```bash
./gradlew assembleDebug
```

Debug APK output:
- `app/build/outputs/apk/debug/app-debug.apk`

## Project Structure

```text
app/src/main/java/com/studysphere/
  data/
    db/
    models/
    repository/
  ui/
    components/
    screens/
    theme/
    Navigation.kt
    StudySphereApp.kt
  viewmodel/
    MainViewModel.kt
```

## Data and Privacy

StudySphere is offline-first. Data is stored locally on the device using Room and DataStore.
No account or network connection is required for core functionality.
