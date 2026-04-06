# StudBuddy — Android Student Companion App

## Project Overview

StudBuddy is a comprehensive Android application designed to help students manage their academic life efficiently. It integrates course management, timetable scheduling, attendance tracking, assignment management, exam preparation, and GPA calculation into a single platform.

---

## Key Features

- **Dashboard**: Centralized view of current semester status, upcoming lectures, short attendance alerts, pending assignments, next exams, and expected GPA.
- **Course Management**: Track courses with credit hours and instructor details.
- **Weekly Timetable**: Weekly schedule management with reminders for upcoming classes.
- **Attendance Tracker**: Monitor attendance percentages against thresholds.
- **Assignment & Exam Tracking**: Manage deadlines and grades for all assessments.
- **GPA Calculator**: Track CGPA and calculate semester GPA based on course grades and credit hours.
- **Settings**: Personalize the app with dark/light mode and notification preferences.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM with Clean Architecture principles |
| UI | XML Layouts (Material Design 3) |
| Persistence | Room Database (SQLite) |
| Navigation | Navigation Component (Single Activity) |
| Networking | Firebase (Future Integration for Sync) |
| Background | WorkManager & AlarmManager |

---

## Project Structure

```
StudBuddy/
└── app/src/main/java/com/example/studbuddy/
    ├── core/                ← Database, Repository, Models, Notifications
    ├── home/                ← Dashboard & Semester logic
    ├── courses/             ← Course management
    ├── attendance/          ← Attendance tracking
    ├── timetable/           ← Schedule management
    ├── assignments/         ← Task management
    ├── exams/               ← Assessment management
    ├── gpa/                 ← Grade calculation
    └── settings/            ← App configurations
```

---

## Documentation

- [Architecture Overview](file:///home/ahmad/Study/Sem 6/MAD/Studbuddy/ARCHITECTURE.md)
- [Improvement Roadmap](file:///home/ahmad/Study/Sem 6/MAD/Studbuddy/Improvements.md)
