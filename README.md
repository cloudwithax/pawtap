<h1 align="center">🐾 Pawtap</h1>
<p align="center"><b>Button-to-touch remapper for the Anbernic RG DS (Android 14)</b></p>

Pawtap teaches any physical button on your RG DS to tap a spot on the **top** or **bottom** screen.
Hold the button, the finger stays down; let go, it lifts — so it works for hold/drag too.

## How to use
1. Install the APK from [Releases](../../releases) and open Pawtap.
2. Tap **Wake up Pawtap** and enable it in Accessibility settings.
3. **+ New remap** → **Listen for a button** → press A/B/L/whatever.
4. Choose **Top screen** or **Bottom screen**, then **Pick a spot on that screen** and tap where the press should land (or type x/y).
5. Save. Toggle **Remaps active** any time to pause everything.

## How it works
An `AccessibilityService` with `flagRequestFilterKeyEvents` catches the button and injects a touch on the chosen display via
`dispatchGesture(GestureDescription.Builder().setDisplayId(...))`. No root needed.

## Building
```
./gradlew assembleDebug        # app/build/outputs/apk/debug/app-debug.apk
```
Requires JDK 17+ and Android SDK 34.

Sibling project: [Taizi](https://github.com/cloudwithax/taizi), a launcher for the same handhelds.
