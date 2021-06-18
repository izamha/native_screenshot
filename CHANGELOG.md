## 0.0.5 - 25.12.2020
- Fixed `README.md` style consistency.
### Android
- Fixed the missing directory path to save the screenshot in release mode.
- Added `android:requestLegacyExternalStorage="true"` to `README.md` to fix temporary the permissions issue.

---

## 0.0.4 - 15.05.2020
- Homepage, Repository and Issuetracker updated to GitLab ones.
### Android
- Added some log messages to make easier to track errors.
- Class names minified/obfuscated by Android release mode were preventing get the correct class of the needed view for take the screenshot. Fixed comparing classes instead names. ivanjpg/native_screenshot#2

---

## 0.0.3
- Homepage, Repository and Issuetracker added to `pubspec.yaml`.
### iOS
- Code improvement.
- The function now takes the screenshot, save it to the application directory and then it adds to the PhotoLibrary. Now it needs `NSPhotoLibraryAddUsageDescription` key into `info.plist` file.

---

## 0.0.2
- Description added to `pubspec.yaml`.
- Documentation API done.
- Added licence.
- `README.md` updated

---

### Android
- Unified the code to condesate the plugin initialization and use for the v1 and v2 Flutter's plugin engine.
- Now the screenshot is written to a folder with the application name on it, if fails then goes to external storage.
- Tried `Canvas` and `PixelCopy` approaches, both throws a black image, still uses `getBitmap()` method.

---

## 0.0.1
### iOS
- Basic functionionality done.

### Android
- Tries to request permission if possible and considers `PixelCopy` (_API >= 26_) that still not works; `getBitmap()` works, so this is used for now.

---