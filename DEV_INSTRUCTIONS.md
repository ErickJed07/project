# Developer Workflow & Automation Rules

## Phindee Update Automation
When the user says **"update phindee"**, the agent must:
1.  **Increment** the `versionCode` (integer) and `versionName` (e.g., 1.2 -> 1.3) in `app/build.gradle.kts`.
2.  **Update** the `app-updates/version.json` file with the new `version_code` and `version_name`.
3.  **Gradle Sync**: Run the `gradle_sync` tool.
4.  **Run Gradle Build**: Execute `./gradlew :app:assembleDebug`.
5.  **Copy APK**: Move the newly built APK from `app/build/outputs/apk/debug/app-debug.apk` to `app-updates/app-debug.apk`.

**Note:** The user will handle the `git commit` and `git push` manually.

## Feature Status
- **Update UI (D_FeedActivity.java)**: Currently **HIDDEN/COMMENTED OUT**. Do not re-enable until specifically requested by the user.
