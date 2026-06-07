# StudyBuddy — System Architecture

---

## 1. Architecture Overview

StudBuddy follows a **Clean Architecture** approach using **MVVM (Model-View-ViewModel)**. The app is structured as a single-activity application using the **Navigation Component** and **Hilt** for dependency injection.

```
┌──────────────────────────────────────────────────────────────────┐
│                          UI Layer                                │
│     MainActivity (Single Activity) + DrawerLayout Sidebar        │
│          ↕                 ↕                    ↕                │
│   HomeFragment    CoursesFragment    AttendanceFragment  ...     │
│   SettingsFragment → ProfileFragment                             │
│                   → NotificationSettingsFragment                 │
│                   → AppearanceSettingsFragment                   │
│                   → CloudSyncFragment          ← NEW             │
└──────────────────────────┬───────────────────────────────────────┘
                           │  observes via StateFlow / LiveData
┌──────────────────────────▼───────────────────────────────────────┐
│                       Domain Layer                               │
│              ViewModels (HiltViewModel, @Inject)                 │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│                       Data Layer                                 │
│   StudBuddyRepository  ←——→  SyncRepository                     │
│         ↓                          ↓                             │
│   Room Database              Firebase Firestore                  │
│   (Local Persistence)        (Cloud Persistence)                 │
└──────────────────────────┬───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│                      System Layer                                │
│      WorkManager (SyncWorker, DailyMaintenanceWorker)            │
│      AlarmManager + AlarmReceiver (Lecture Reminders)            │
│      NotificationHelper + NotificationScheduler                  │
└──────────────────────────────────────────────────────────────────┘
```

---

## 2. Component Responsibilities

### 2.1 StudBuddyApp (Application Class)

- Initializes **Hilt** via `@HiltAndroidApp`.
- Implements `Configuration.Provider` to supply the **HiltWorkerFactory** to WorkManager.
- On startup, schedules:
  - `SyncWorker` — periodic Firebase sync every 1 hour (network-constrained).
  - `DailyMaintenanceWorker` — daily notification maintenance at user-configured time.
- Initializes the local guest user via `UserManager` if no user is present.

### 2.2 MainActivity

- **Host**: Manages the `NavHostFragment` and the global `DrawerLayout`.
- **Navigation**: Uses `NavigationView` with `NavController` to handle fragment transitions with a custom `setNavigationItemSelectedListener` (ensuring sidebar stays in sync on back-press and argument-based navigation).
- **Theme**: Observes `SettingsManager.themeMode` via `lifecycleScope` and applies it at runtime.
- **User Header**: Observes `UserManager.userFlow` to display the user's name, email, and profile photo in the nav drawer header (loaded with Coil).
- **Notifications**: Creates notification channels and requests `POST_NOTIFICATIONS` permission on Android 13+.

### 2.3 Fragments (Feature Views)

Each feature is a `Fragment` paired with a `@HiltViewModel`:

| Fragment | Responsibility |
|---|---|
| **HomeFragment** | Dashboard — upcoming lectures, short attendance, pending assignments, next exam, semester GPA |
| **SemesterFragment** | CRUD for semesters, set active semester |
| **CoursesFragment** | Course list (filtered by active or specified semester) |
| **CourseDetailFragment** | Timetable, attendance, assignments, exams, and notes per course |
| **AttendanceFragment** | All attendance records with percentage per course |
| **TimetableFragment** | Weekly timetable view |
| **AssignmentsFragment** | Pending and completed assignment list |
| **ExamsFragment** | Upcoming and completed exam list |
| **GpaFragment** | Per-semester GPA and cumulative CGPA |
| **ProfileFragment** | User display name, profile photo, Google Sign-In / Sign-Out |
| **SettingsFragment** | Entry point to sub-settings pages |
| **NotificationSettingsFragment** | Class reminders, assignment and exam lead times |
| **AppearanceSettingsFragment** | Light / Dark / System theme |
| **CloudSyncFragment** | Manual sync trigger, sync status display, last sync time |

### 2.4 Data Layer

#### StudBuddyRepository
Single source of truth for all **local** data operations. Wraps Room DAOs and exposes `Flow`-based reactive streams to ViewModels. Also handles GPA recalculation when course grades change.

#### SyncRepository
Handles **bidirectional sync** between Room (local) and Firebase Firestore (remote):
- **Push**: All local entities are written to Firestore under `users/{uid}/{collection}/{id}` using explicit `Map`-based serialization (not `toObject()`, to avoid Kotlin data class deserialization issues).
- **Pull**: Remote documents newer than their local counterpart (by `lastModified` timestamp) are written back to Room.
- **Sync scope**: Semesters, Courses, Timetable, Attendance, Assignments, Exams. Notes are local-only.
- **Error handling**: Each table sync is isolated in its own `try/catch`. Any table failure is logged and re-thrown at the end so callers can show an error state.
- Records last successful sync time via `SettingsManager.setLastSyncTime()`.

### 2.5 Room Database (`StudBuddyDatabase`)

Version **6**, with `fallbackToDestructiveMigration`. Contains:

| Entity | Table | Description |
|---|---|---|
| `Semester` | `semesters` | Semester date range, active flag, GPA |
| `Course` | `courses` | Course details with `semesterId` FK |
| `TimetableEntry` | `timetable_entries` | Day/time/room slot with `courseId` FK |
| `AttendanceRecord` | `attendance_records` | Present/Absent/Late log with `courseId` FK |
| `Assignment` | `assignments` | Due date, marks, completion with `courseId` FK |
| `Exam` | `exams` | Quiz/Midterm/Final with `courseId` FK |
| `Note` | `notes` | Local file reference with `courseId` FK (local-only, not synced) |

All entities carry a `lastModified: Long` timestamp used for sync conflict resolution.

### 2.6 Firebase Integration

| Service | Usage |
|---|---|
| **Firebase Auth** | Google Sign-In via `GoogleSignInOptions`. UID used as the Firestore root document key. |
| **Cloud Firestore** | Hierarchical data under `users/{uid}/{collection}/{docId}`. |

Firestore data path structure:
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

> **Firestore Security Rules required:**
> ```javascript
> rules_version = '2';
> service cloud.firestore {
>   match /databases/{database}/documents {
>     match /users/{userId}/{document=**} {
>       allow read, write: if request.auth != null && request.auth.uid == userId;
>     }
>   }
> }
> ```

### 2.7 Background Workers (WorkManager)

| Worker | Trigger | Responsibility |
|---|---|---|
| `SyncWorker` | Periodic — every 1 hour (network required) | Calls `SyncRepository.syncAll()`. Retries up to 3 times on failure. |
| `DailyMaintenanceWorker` | Periodic — every 24 hours at configured time | Reschedules lecture, assignment, and exam notifications. |

### 2.8 Notification System

| Component | Role |
|---|---|
| `NotificationHelper` | Creates notification channels; posts notifications |
| `NotificationScheduler` | Schedules exact alarms via `AlarmManager` for lectures, assignments, exams |
| `AlarmReceiver` | BroadcastReceiver — fires when alarm triggers, posts the notification |
| `BootReceiver` | BroadcastReceiver — reschedules alarms after device reboot |
| `NotificationEntryPoint` | Hilt entry point for injection inside `AlarmReceiver` |

### 2.9 Persistence & Settings

| Manager | Storage | Responsibility |
|---|---|---|
| `SettingsManager` | DataStore Preferences (`studbuddy_settings`) | Theme mode, notification preferences, last sync timestamp |
| `UserManager` | DataStore Preferences (`user_prefs`) | User ID, display name, email, profile image URI, auth status |

---

## 3. Data Flow

### Normal Write (e.g., Adding a Semester)

```
User taps "Add"
  → SemesterFragment calls SemesterViewModel.saveSemester()
  → SemesterViewModel calls StudBuddyRepository.saveSemester()
  → Repository calls SemesterDao.insert()
  → Room emits update on getAllFlow()
  → SemesterFragment RecyclerView auto-updates
```

### Cloud Sync Flow (Manual)

```
User taps "Sync Now" in CloudSyncFragment
  → CloudSyncViewModel.triggerSync()
  → viewModelScope.launch { syncRepository.syncAll() }
    → Push: all local items → Firestore set()
    → Pull: remote items newer than local → Room insert()
  → syncStatus = SUCCESS / ERROR
  → SettingsManager.setLastSyncTime() updated
  → UI reflects new status and last sync time
```

### Cloud Sync Flow (Automatic)

```
WorkManager fires SyncWorker every 1 hour (when network available)
  → SyncWorker.doWork() calls SyncRepository.syncAll()
  → Same push/pull logic as above
  → Result.success() or Result.retry() (up to 3 attempts)
```

---

## 4. Dependency Injection (Hilt)

| Module | Provides |
|---|---|
| `DatabaseModule` | `StudBuddyDatabase`, all DAOs |
| `RepositoryModule` | `StudBuddyRepository`, `SyncRepository`, `UserManager`, `SettingsManager` |

All ViewModels are `@HiltViewModel` with `@Inject constructor`. Workers are `@HiltWorker` with `@AssistedInject`.

---

## 5. Navigation Structure

```
nav_graph.xml
├── homeFragment (start)
├── semesterFragment
├── coursesFragment  →  courseDetailFragment
│                           └── [tab: notes, attendance, assignments, exams]
├── attendanceFragment
├── timetableFragment
├── assignmentsFragment
├── examsFragment
├── gpaFragment
├── profileFragment
└── settingsFragment
        ├── → notificationSettingsFragment
        ├── → appearanceSettingsFragment
        └── → cloudSyncFragment
```

Navigation is handled through the DrawerLayout sidebar. Back-stack and single-top behaviour are managed per destination.

---

## 6. Key Design Decisions

| Decision | Rationale |
|---|---|
| Map-based Firestore serialization | Kotlin data classes with Room annotations lack no-arg constructors, making `toObject()` unreliable |
| `lastModified` for conflict resolution | Simple last-write-wins; avoids CRDTs for a student scope app |
| Notes excluded from sync | Notes reference local file paths that are device-specific |
| `SyncRepository` separate from `StudBuddyRepository` | Keeps local and remote concerns isolated |
| Manual sync runs in `viewModelScope` | Gives direct, reliable UI feedback; WorkManager used only for background periodic sync |
| `fallbackToDestructiveMigration` | Acceptable during active development; should be replaced with proper migrations before production |
