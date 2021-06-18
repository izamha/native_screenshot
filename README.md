# native_screenshot

This plugin aims to be a simple one that implements taking screenshot natively to capture scenes like camera preview or AR views. This cannot be done easily in plain Flutter, at least I cannot make it work using `RenderRepaintBoundary` and similar techniques.

The main difference with another packages is that they shows a share dialog. This plugin saves the image and returns the path to it. On **Android** also launch an updating request (internally) to reload the media library.

## Instalation

Add

```
native_screenshot: ^<latest_version>
```

to your `pubspec.yaml` file.

### Android
You must add

```
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

to your `AndroidManifest.xml` inside `android/src/main/` directory.

### iOS
If don't add

```
<key>NSPhotoLibraryAddUsageDescription</key>
<string>Take pretty screenshots and save it to the PhotoLibrary.</string>
```

to your `info.plist` file inside `ios/Runner` directory, the application will crash.

## Use

Import the library:

```
import 'package:native_screenshot/native_screenshot.dart';
```

and take a screenshot:

```
String path = await NativeScreenshot.takeScreenshot()
```

In error case the function returns `null` and the screenshot path if success.

## Acknowledgments
This is based on [screenshot_share_image](https://pub.dev/packages/screenshot_share_image) and [capture_and_share](https://pub.dev/packages/capture_and_share) packages. Thanks to [@toonztudio](https://github.com/toonztudio) for pointing me out replying in Github.
