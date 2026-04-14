# StudySphere — Accessible Academic Planner

A modern, accessibility-focused Android academic planning app built with **Kotlin + Jetpack Compose**.

## 📦 Download APK (From GitHub README)

[Download StudySphere v1.0.0 APK](https://github.com/Chirag8405/StudySphere/releases/download/1.0.0/StudySphere.apk)

This direct link targets release tag `1.0.0` and requires the APK asset with the exact filename:
- `StudySphere.apk`

If the uploaded asset name is different, update the link accordingly.

---

## ✨ Features

### 🎓 Subjects
- Add, edit, and delete subjects with custom colors (12-color palette)
- Set per-subject minimum attendance threshold (50–100%)
- Define recurring weekly lecture slots with day, time, and room

### 📋 Attendance Management
- Dashboard shows today's lectures with quick **P / A / C** mark buttons
- Subject-wise attendance summaries with live progress bars
- Mark attendance date via a calendar date picker (no manual date typing)
- Intelligence engine:
  - **Can skip N more** — classes you can miss while staying above threshold
  - **Must attend N** — consecutive classes needed to recover
  - Risk levels: Safe · Warning · Danger · Critical
- Full history view per subject with inline status editing

### 📝 Assignments
- Create assignments with title, description, due date, and priority
- Add assignments under a subject or an **Other** category
- Pick due date from a calendar date picker
- Search assignments + 2 dropdown filters (status/priority and subject)
- One-tap complete/undo from the card checkbox
- Overdue detection with visual urgency cues

### 🏠 Dashboard
- Today's schedule with instant attendance marking
- 4-stat quick-glance row (subjects, avg %, due soon, at risk)
- Attendance health summary with smart insight banners
- Upcoming deadlines preview
- Pull-to-refresh gesture support

### 🌙 Theming
- Full **Light / Dark** theme toggle (persisted across sessions)
- Material 3 color system with semantic states
- Prefers **SF Pro Display** for UI text and **SF Mono** for compact labels (with Android-safe fallbacks)

---

## 🏗️ Architecture

```
com.studysphere/
├── data/
│   ├── db/          — Room DAOs, Database, TypeConverters
│   ├── models/      — Entity & view models
│   └── repository/  — StudySphereRepository (all business logic)
├── ui/
│   ├── components/  — Shared Compose components
│   ├── screens/
│   │   ├── dashboard/
│   │   ├── attendance/
│   │   ├── assignments/
│   │   └── SubjectsScreen
│   ├── theme/       — Color, Typography, Theme
│   ├── Navigation   — Route definitions
│   └── StudySphereApp — Scaffold + NavHost
└── viewmodel/       — MainViewModel (single source of truth)
```

**Stack:** Kotlin · Jetpack Compose · Room · DataStore · Navigation Compose · Material 3 · Coroutines + Flow

---

## 🚀 Setup

### Prerequisites
- **Android Studio Hedgehog** (2023.1.1) or newer
- **JDK 17**
- Android SDK 34, min SDK 26

### Steps

1. **Unzip** this project and open in Android Studio:
   ```
   File → Open → select the StudySphere/ folder
   ```

2. **Sync Gradle** — Android Studio will prompt automatically. Click *Sync Now*.

3. **Run** on a device or emulator (API 26+):
   ```
   Run → Run 'app'
   ```

### Publish APK For v1.0.0 Release

1. Build APK:
   ```bash
   ./gradlew assembleRelease
   ```
2. Create a GitHub Release with tag:
   - `1.0.0`
3. Upload `app/build/outputs/apk/release/app-release-unsigned.apk` and rename it to:
   - `StudySphere.apk`
4. The README download link above will start working immediately.

### gradlew wrapper jar

If you see `Could not find or load main class org.gradle.wrapper.GradleWrapperMain`:
```bash
# Run once inside the project root
gradle wrapper --gradle-version 8.2.1
```
Or let Android Studio auto-download it on first sync.

---

## 📱 Navigation

| Screen      | Route                | Description                          |
|-------------|----------------------|--------------------------------------|
| Dashboard   | `dashboard`          | Today's schedule + health overview   |
| Attendance  | `attendance`         | All subjects with risk analysis      |
| Detail      | `attendance_detail/{id}` | Per-subject history + mark dialog |
| Assignments | `assignments`        | Full task list with filters          |
| Subjects    | `subjects`           | Subject + lecture slot management    |

---

## 🗃️ Local Storage

All data is stored on-device using:
- **Room Database** (`studysphere.db`) — subjects, lectures, attendance records, assignments
- **DataStore Preferences** — theme toggle (dark/light)

No account, login, or network required.

---

## 🧠 Attendance Intelligence Algorithm

Given:
- `present` = classes attended
- `total` = non-cancelled classes held
- `minPct` = minimum threshold (e.g. 0.75)

**Can Skip:**
```
canSkip = floor(present / minPct) - total   [min 0]
```

**Must Attend (to recover):**
```
mustAttend = ceil((minPct × total − present) / (1 − minPct))
```

**Risk Level:**
| Condition | Level |
|-----------|-------|
| pct ≥ threshold + 15% | Safe |
| pct ≥ threshold | Warning |
| pct ≥ threshold − 10% | Danger |
| pct < threshold − 10% | Critical |

---

## 🎨 Design System

Colors follow a restrained indigo-primary palette with semantic states:
- **Success** `#22C55E` — safe / present
- **Warning** `#F59E0B` — borderline / medium priority
- **Danger**  `#EF4444` — below threshold / overdue
- **Primary** `#4F46E5` — brand indigo

Dark theme uses deep navy backgrounds (`#0C0F1A`, `#131827`, `#1A2035`) with the same indigo accent.
