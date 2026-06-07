# StudyBuddy — Student Companion App

A comprehensive Android app that helps students manage their academic life — courses, timetable, attendance, assignments, exams, GPA, and cloud sync — all in one place.

---

## Features

| Feature | Details |
|---|---|
| **Dashboard** | Semester overview, upcoming lectures, attendance alerts, pending assignments, next exam, expected GPA |
| **Semester Management** | Create semesters with date ranges, set an active semester |
| **Course Management** | Track courses with credit hours, instructor, and grades per semester |
| **Weekly Timetable** | Add class slots with day, time, room, and colour; get lecture reminders |
| **Attendance Tracker** | Mark Present / Absent / Late per lecture; view attendance % per course |
| **Assignments** | Create assignments with due dates, marks, and weightage; mark as complete |
| **Exams** | Schedule Quiz, Midterm, and Final exams with venue and marks |
| **GPA Calculator** | Auto-calculates semester GPA and cumulative CGPA from course grades |
| **Notes** | Attach local files (PDFs, images) to each course |
| **Cloud Sync** | Bidirectional Firebase Firestore sync; manual "Sync Now" + automatic hourly background sync |
| **Profile** | Google Sign-In, display name, and profile photo |
| **Appearance** | Light / Dark / System theme |
| **Notifications** | Configurable lecture reminders, assignment deadlines, and exam alerts |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM + Clean Architecture |
| UI | XML Layouts, Material Design 3 |
| Local Persistence | Room Database (SQLite) |
| Cloud Persistence | Firebase Firestore |
| Authentication | Firebase Auth (Google Sign-In) |
| Dependency Injection | Hilt |
| Navigation | Navigation Component (Single Activity) |
| Background Tasks | WorkManager (`SyncWorker`, `DailyMaintenanceWorker`) |
| Scheduling | AlarmManager (exact lecture/exam/assignment reminders) |
| Image Loading | Coil |
| Preferences | Jetpack DataStore |

---

## Project Structure

```
app/src/main/java/com/example/studbuddy/
│
├── core/
│   ├── db/               ← Room database, DAOs, TypeConverters
│   ├── models/           ← Data entities (Semester, Course, Exam, …)
│   ├── repository/
│   │   ├── StudBuddyRepository.kt   ← Local data (Room)
│   │   └── SyncRepository.kt        ← Firebase Firestore sync
│   ├── di/               ← Hilt modules (DatabaseModule, RepositoryModule)
│   ├── notifications/    ← NotificationHelper, Scheduler, AlarmReceiver, BootReceiver
│   ├── workers/          ← SyncWorker, DailyMaintenanceWorker
│   ├── SettingsManager.kt
│   └── UserManager.kt
│
├── home/                 ← Dashboard
├── semesters/            ← Semester management
├── courses/              ← Course list + CourseDetail (tabs for notes, attendance, etc.)
├── attendance/           ← Attendance tracking
├── timetable/            ← Weekly schedule
├── assignments/          ← Assignment tracking
├── exams/                ← Exam management
├── gpa/                  ← GPA calculator
├── notes/                ← Local file notes per course
├── profile/              ← User profile + Google Sign-In
└── settings/
    ├── SettingsFragment.kt
    ├── AppearanceSettingsFragment.kt
    ├── NotificationSettingsFragment.kt
    ├── CloudSyncFragment.kt          ← Sync status, last sync time, Sync Now button
    └── CloudSyncViewModel.kt
```

---

## Firebase Setup

### Prerequisites
1. Create a Firebase project at [console.firebase.google.com](https://console.firebase.google.com).
2. Add an Android app with package name `com.example.studbuddy`.
3. Download `google-services.json` and place it in `app/`.
4. Enable **Google Sign-In** under Authentication → Sign-in method.
5. Create a **Firestore Database** (start in production mode).

### Firestore Security Rules

Set the following rules in **Firestore → Rules** so each user can only access their own data:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /users/{userId}/{document=**} {
      allow read, write: if request.auth != null && request.auth.uid == userId;
    }
  }
}
```

### Firestore Data Structure

```
users/
  {userId}/
    semesters/{semesterId}
    courses/{courseId}
    timetable/{entryId}
    attendance/{recordId}
    assignments/{assignmentId}
    exams/{examId}
```

> **Notes are not synced** — they reference local device file paths.

---

## Getting Started

```bash
# Clone the repo
git clone <repo-url>
cd Studbuddy

# Open in Android Studio, let Gradle sync
# Place your google-services.json in app/
# Build & Run on a device or emulator (API 24+)
```

### Requirements
- Android Studio Hedgehog or later
- Android SDK 36 (compileSdk), minSdk 24
- A Firebase project with Firestore and Google Auth enabled

---

## How Sync Works

1. **Sign in** via Profile → Google Sign-In.
2. **Automatic**: `SyncWorker` runs every hour in the background when connected to the internet.
3. **Manual**: Settings → Cloud Sync → **Sync Now**.
4. The sync page shows the current sync status and the last successful sync time.
5. Conflict resolution uses `lastModified` timestamps — the newer record wins.

---

## Documentation

- [Architecture Overview](ARCHITECTURE.md)
