# Notification Manager

## Introduction

Notification Manager is an Android app that gives you full control over the notifications on your device.  
It lets you **silence** (receive the notification without sound or vibration) or **block** (remove the notification completely) any installed application for a custom period of time.

Apps can be organized into **Groups**, so a single action can silence or block many apps at once.  
The app also supports **Triggers**, which are automated rules that activate a silence or block condition based on:

- **Time of day** – a trigger fires every day at a configured hour and minute.
- **Notification keyword** – a trigger fires when a notification from a specific app or group contains a given keyword.

A **persistent notification** keeps you informed while timers are active, and includes a button to deactivate all rules from the notification shade without opening the app.

The entire UI is built with **Jetpack Compose** and follows Material 3 design guidelines.

---

## Version History

### V1.6 – Background Execution & Triggers V2
- Persistent notification while timers are active ("Notification Manager Active") with a "Deactivate all" button.
- Timer dialog redesigned: custom hour (0-23) and minute (0-59) input fields instead of fixed time buttons.
- Triggers now support **scheduled time** (fires daily at a configured hour) and **keyword matching** (fires when a notification contains a word).
- Expandable trigger items in the Triggers screen.
- Trigger Wizard tabs for selecting apps and groups.
- Code commented in simple English following the project's commenting style.

### V1.5 – Triggers & Navigation Overhaul
- New **Triggers** section in the vertical menu to create and manage automated silence/block rules.
- Trigger Wizard with step-by-step configuration (select apps/groups, choose type, set duration).
- **Groups** moved to the bottom navigation bar (between Home and Silenced).
- Dynamic top bar title shows the name of the current section instead of "Notification Manager".
- Confirmation dialog before removing an app from a group.
- Fixed Home screen lag caused by loading apps on the main thread.

### V1.4 – Permissions & Silent Notifications
- Added `POST_NOTIFICATIONS` permission request for Android 13+.
- Warning banner when notification permission is not granted.
- Fixed silent notification reposting: uses our own small icon to prevent Android from dropping the notification.

### V1.3 – Groups & Silence Improvements
- Expandable group cards that show the list of apps inside a group with a remove button.
- Editable group name field in the edit dialog.
- Fixed the silence feature so notifications arrive without sound or vibration instead of being hidden.

### V1.2 – Core Blocking & Silencing
- Block and silence individual apps with a configurable timer.
- Group management: create, edit, and delete groups of apps.
- Check marks visible when selecting apps for a group.
- Horizontal and vertical menu navigation working correctly.

### V1.1 – Initial UI
- Home screen listing all installed apps.
- Silenced and Blocked screens.
- Settings screen with theme switching (System / Light / Dark).
- Basic notification listener service.

### V1.0 – Project Setup
- Initial Android project created with Jetpack Compose.
- Navigation structure with Scaffold, TopAppBar, and BottomNavigationBar.
- SharedPreferences-based data persistence with Gson.

---

## How to Download

You can download the latest APK directly from the repository:

**[Download Notification-manager-V1.6](https://github.com/ejnicdia/Notification-manager/blob/main/app/build/outputs/apk/debug/app-debug.apk)**

> To install the APK on your device, you may need to enable "Install from unknown sources" in your Android settings.