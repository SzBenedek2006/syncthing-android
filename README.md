<div align="center">

# Syncthing for Android

[![License: MPLv2](https://img.shields.io/badge/License-MPLv2-blue.svg)](https://opensource.org/licenses/MPL-2.0)
[![Build](https://img.shields.io/github/actions/workflow/status/SzBenedek2006/syncthing-android/android.yml?logo=github)](https://github.com/SzBenedek2006/syncthing-android/actions)
[![Min SDK](https://img.shields.io/badge/minSdk-23(Android%206)-38a853?logo=android)](https://developer.android.com/about/versions/marshmallow)

A wrapper of [Syncthing](https://github.com/syncthing/syncthing) for Android.
This is intended to be the revival and continuation of the original [syncthing/syncthing-android](https://github.com/syncthing/syncthing-android) project.

I want to thank all contributors of the original project for their awesome work!

<!--
<img src="app/src/main/play/listings/en-GB/graphics/phone-screenshots/screenshot_phone_1.png" alt="screenshot 1" width="200" /> <img src="app/src/main/play/listings/en-GB/graphics/phone-screenshots/screenshot_phone_2.png" alt="screenshot 2" width="200" /> <img src="app/src/main/play/listings/en-GB/graphics/phone-screenshots/screenshot_phone_3.png" alt="screenshot 3" width="200" />
-->
<img width="200" alt="Welcome screen of the app (white mode)" src="https://github.com/user-attachments/assets/2a726ce3-edfe-4fec-96d8-0696323362fb" />
<img width="200" alt="Welcome screen of the app (dark mode)" src="https://github.com/user-attachments/assets/3a922a9b-3d84-4bfe-bad2-9b26518fc086" />

</div>


# Building
_(May be out of date. If so, please open an issue!)_

These dependencies and instructions are necessary for building from the command
line. If you build using Android Studio, you can set up SDK, Java and NDK differently.
The Docker file is not needed for building, and is out of date or non-functional.

## Dependencies

1. Set up JDK version 21.
2. Android SDK and NDK
    1. Download SDK command line tools from https://developer.android.com/studio#command-line-tools-only.
    2. Unpack the downloaded archive to an empty folder. This path is going
       to become your `ANDROID_HOME` folder.
    3. Inside the unpacked `cmdline-tools` folder, create yet another folder
       called `latest`, then move everything else inside it, so that the final
       folder hierarchy looks as follows.
       ```
       cmdline-tools/latest/bin
       cmdline-tools/latest/lib
       cmdline-tools/latest/source.properties
       cmdline-tools/latest/NOTICE.txt
       ```
    4. Navigate inside `cmdline-tools/latest/bin`, then execute
       ```
       yes | ./sdkmanager "platform-tools" "build-tools;36.0.0" "platforms;android-36" "extras;android;m2repository" "ndk;29.0.14206865"
       ```
       The required tools and NDK will be downloaded automatically.

_(Go is downloaded automatically by Gradle when building.)_
_(Python is not needed anymore.)_


## Build instructions

1. Clone the project with
    ```
    git clone https://github.com/syncthing/syncthing-android.git --recursive
    ```
    Alternatively, if already present on the disk, run
    ```
    git pull && git submodule init && git submodule update
    ```
    in the project folder.
2. Make sure that the `ANDROID_HOME` environment variable is set to the path
   containing the Android SDK (see [Dependencies](#dependencies)).
3. Navigate inside `syncthing-android`, then build with

    To build the APK:
    ```
    ./gradlew assembleDebug
    ```
    To build the AAB:
    ```
    ./gradlew bundleDebug
4. Once completed, the result will be in `app/build/outputs/apk/debug` and `app/build/outputs/bundle/debug` respectively.

**NOTE:** On Windows, you must use the Command Prompt (and not PowerShell) to
compile. When doing so, in the commands replace all forward slashes `/` with
backslashes `\`.

# License

The project is licensed under the [MPLv2](LICENSE).
