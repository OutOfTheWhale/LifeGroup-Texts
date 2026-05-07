# Life Group Texts

A simple Android app for sending SMS reminders to your church life group.

## Build the APK (local — no EAS needed)

Run these commands in order inside this folder:

```
npm install
npm run build
npx cap add android
npx cap sync
npx cap open android
```

Then in Android Studio:
- Wait for Gradle to finish syncing (bottom status bar)
- Go to **Build → Build Bundle/APK → Build APK(s)**
- Click the APK link when it pops up in the bottom right
- Done! APK is in `android/app/build/outputs/apk/debug/`

## Install on your phone

```
adb install android/app/build/outputs/apk/debug/app-debug.apk
```

Or transfer the APK file to your phone and open it with a file manager
(enable "Install from unknown sources" in Android settings first).
