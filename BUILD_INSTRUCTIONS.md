# Building iBoost X

There are two ways to get a working APK from this project. Since the whole
project was built for a mobile-only, Termux + GitHub workflow, **Option A
is the one to use.**

---

## Option A — Build automatically on GitHub (no Android Studio, no laptop)

Every push to the `main` branch triggers `.github/workflows/build-apk.yml`,
which builds the app on GitHub's servers and hands you back a real APK.

### One-time setup

1. Push this project to a new GitHub repo:
   ```
   cd iBoostX
   git init
   git add .
   git commit -m "Initial commit: iBoost X"
   git branch -M main
   git remote add origin https://github.com/<your-username>/<your-repo>.git
   git push -u origin main
   ```
2. Go to your repo on GitHub → the **Actions** tab. You'll see "Build
   iBoost X APK" running automatically.
3. When it finishes (green check), open that run → scroll to
   **Artifacts** → download `iBoostX-debug-apk`. That's a real,
   installable, debug-signed APK.
4. Transfer it to your phone (GitHub app, browser download, `curl`/`wget`
   in Termux, whatever's convenient) and install it — you'll need to
   allow "install unknown apps" for whichever app you download it with.

That's the whole loop: edit code in Termux → `git push` → download the
APK from the Actions tab a couple minutes later.

### Getting a signed *release* APK instead of debug

The debug APK works fine for your own testing, but if you want a proper
signed release build (smaller, optimized, ready to share), add a keystore
as a GitHub Secret:

1. Generate a keystore once (this can be done in Termux with `keytool`,
   which ships with any JDK — install one via `pkg install openjdk-17` if
   you don't have it):
   ```
   keytool -genkeypair -v -keystore release.keystore -alias iboostx \
     -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Base64-encode it:
   ```
   base64 -w0 release.keystore > release.keystore.b64
   ```
3. In your GitHub repo: **Settings → Secrets and variables → Actions →
   New repository secret**, and add these four secrets:
   - `KEYSTORE_BASE64` — the contents of `release.keystore.b64`
   - `KEYSTORE_PASSWORD` — the password you set above
   - `KEY_ALIAS` — `iboostx` (or whatever alias you used)
   - `KEY_PASSWORD` — usually the same as the keystore password
4. Push again (or re-run the workflow). The workflow will now also build
   and upload `iBoostX-release-apk`.

**Keep `release.keystore` itself somewhere safe and never commit it** —
`.gitignore` already excludes it, and if you lose it you won't be able to
publish updates under the same signature.

---

## Option B — Build locally in Android Studio

1. Install **Android Studio** (Hedgehog/2023.1 or newer) with the
   Android SDK, NDK, and CMake components (SDK Manager → SDK Tools →
   check "NDK (Side by side)" and "CMake").
2. Open the `iBoostX/` folder as a project. Android Studio will generate
   the Gradle wrapper automatically on first sync (it notices
   `gradle-wrapper.properties` and offers to create the wrapper jar).
3. Let Gradle sync — this downloads dependencies and configures the
   native build.
4. Run ▶ on a device/emulator for a debug build, or
   **Build → Generate Signed Bundle / APK** for a signed release.

### Command line, once the wrapper exists

```
./gradlew assembleDebug      # debug APK
./gradlew assembleRelease    # release APK (needs a signing config)
```

Output APKs land in `app/build/outputs/apk/debug/` or
`app/build/outputs/apk/release/`.

---

## Why there's no committed `gradlew` binary

`gradlew` normally ships with a small binary jar
(`gradle/wrapper/gradle-wrapper.jar`). That binary can't be generated
from here, so it's left out — Android Studio creates it automatically on
first open, and the GitHub Actions workflow sidesteps it entirely by
installing Gradle directly on the runner. Either path gets you a working
build without you needing to hand-create that jar yourself.
