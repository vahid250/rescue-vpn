# پروژه کامل اندروید و راهنمای ساخت فایل نصبی (APK Generator & Project Package)
## اپلیکیشن Rescue VPN AI — فیلترشکن هوشمند، امن و مقاوم در برابر فیلترینگ

برای اینکه فایل نصبی نهایی (**APK** یا **AAB**) را با یک کلیک در سیستم خود یا به صورت آنلاین و رایگان کامپایل و دانلود کنید، تمامی فایل‌های زیرساختی پروژه، سورس Gradle، مانیفست اندروید و اسکریپت بیلد یکپارچه آماده شده است.

---

### ۱. فایل `AndroidManifest.xml` (پیکربندی دسترسی‌ها و سرویس VPN)
مسیر فایل در پروژه: `app/src/main/AndroidManifest.xml`

```xml
<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <!-- دسترسی‌های ضروری شبکه و امنیت -->
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />
    <uses-permission android:name="android.permission.CHANGE_NETWORK_STATE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE"
        tools:ignore="ForegroundServicePermission" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.CAMERA" /> <!-- جهت اسکنر آفلاین QR -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:name=".RescueApp"
        android:allowBackup="false"
        android:icon="@mipmap/ic_launcher"
        android:label="Rescue VPN AI"
        android:roundIcon="@mipmap/ic_launcher_round"
        android:supportsRtl="true"
        android:theme="@style/Theme.RescueVpnAi">

        <!-- صفحه اصلی اپلیکیشن -->
        <activity
            android:name=".ui.MainActivity"
            android:exported="true"
            android:screenOrientation="portrait"
            android:theme="@style/Theme.RescueVpnAi">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <!-- هسته سرویس VpnService اندروید با قابلیت اتصال خودکار -->
        <service
            android:name=".core.vpn.RescueVpnService"
            android:exported="false"
            android:permission="android.permission.BIND_VPN_SERVICE">
            <intent-filter>
                <action android:name="android.net.VpnService" />
            </intent-filter>
        </service>

        <!-- دریافت وضعیت راه‌اندازی پس از ری‌استارت -->
        <receiver
            android:name=".core.receiver.BootReceiver"
            android:enabled="true"
            android:exported="false">
            <intent-filter>
                <action android:name="android.intent.action.BOOT_COMPLETED" />
            </intent-filter>
        </receiver>
    </application>
</manifest>
```

---

### ۲. فایل `app/build.gradle.kts` (تنظیمات کامپایل و کتابخانه‌ها)

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "com.rescuevpn"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.rescuevpn.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0-PROD"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }

        ndk {
            abiFilters += listOf("armeabi-v7a", "arm64-v8a", "x86_64")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug") // جهت بیلد مستقیم و آسان
        }
        debug {
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Jetpack Compose & Material 3
    implementation(platform("androidx.compose:compose-bom:2024.05.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.0")

    // هسته تونل‌سازی و رمزنگاری امن
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("com.google.zxing:core:3.5.3") // اسکن و تولید QR آفلاین
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

---

### ۳. اسکریپت تک‌خطی ساخت خودکار APK (بیلد محلی در سیستم)

#### در لینوکس یا مک (Terminal):
```bash
./gradlew assembleRelease
# یا جهت بیلد نسخه بدون نیاز به امضا:
./gradlew assembleDebug
```
📍 خروجی APK در مسیر: `app/build/outputs/apk/release/app-release.apk` تولید می‌شود.

#### در ویندوز (PowerShell یا CMD):
```powershell
.\gradlew.bat assembleRelease
```

---

### ۴. دانلود مستقیم و خودکار APK از طریق GitHub Actions (بدون نیاز به نرم‌افزار یا سیستم قوی)

اگر Android Studio روی سیستم شما نصب نیست، می‌توانید کدها را در یک ریپازیتوری GitHub (رایگان) قرار دهید تا سرورهای ابری گیت‌هاب ظرف ۲ دقیقه فایل نصبی APK را کامپایل کرده و لینک مستقیم دانلود به شما تحویل دهند:

۱. فایل `.github/workflows/build_apk.yml` (موجود در سند آموزشی) را در پروژه قرار دهید.
۲. با دستور `git push origin main` کدها را ارسال کنید.
۳. در تب **Actions** گیت‌هاب، فایل **`Rescue-VPN-AI-Debug-APK`** آماده دانلود و نصب مستقیم روی گوشی خواهد بود!
