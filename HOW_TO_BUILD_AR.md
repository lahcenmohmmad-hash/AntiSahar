# 🌙 دليل بناء مضاد السهر على GitHub خطوة بخطوة

هذا الدليل بالعربي لـ "نسخ ولصق" بالكامل. لا تحتاج أي برنامج على جهازك سوى متصفح.

---

## 1) إنشاء المستودع على GitHub

1. اذهب إلى https://github.com/new
2. Repository name: `AntiSahar` (أو أي اسم تريده)
3. اختر **Public** أو **Private**
4. **لا تختر** "Add a README" — سنرفعه نحن
5. اضغط **Create repository**

---

## 2) رفع الملفات (الطريقة الأسهل: واجهة GitHub)

1. في صفحة المستودع الفارغ، اضغط **uploading an existing file**
2. اسحب جميع المجلدات والملفات من مجلد `AntiSahar/` دفعة واحدة
   - 💡 إذا واجهتك مشكلة، يمكنك إنشاء كل ملف يدوياً عبر **Add file → Create new file**
   - عندما تكتب اسم الملف، استخدم `/` لإنشاء مجلد. مثال: اكتب `app/src/main/AndroidManifest.xml` فيتم إنشاء المجلدات تلقائياً.
3. اكتب commit message مثل: `initial commit`
4. اضغط **Commit changes**

---

## 3) ترتيب الملفات الذي يجب أن تراه على GitHub

```
AntiSahar/                          ← (جذر المستودع)
├── README.md
├── LICENSE
├── HOW_TO_BUILD_AR.md
├── .gitignore
├── build.gradle
├── settings.gradle
├── gradle.properties
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── .github/
│   └── workflows/
│       └── build.yml
└── app/
    ├── build.gradle
    ├── proguard-rules.pro
    └── src/
        └── main/
            ├── AndroidManifest.xml
            ├── java/com/antisahar/app/
            │   ├── MainActivity.java
            │   ├── data/Prefs.java
            │   ├── receiver/
            │   │   ├── AdminReceiver.java
            │   │   └── BootReceiver.java
            │   ├── service/
            │   │   ├── DailyScheduleWorker.java
            │   │   └── LockAccessibilityService.java
            │   ├── ui/
            │   │   ├── AppsPickerActivity.java
            │   │   ├── LockedScreenActivity.java
            │   │   └── SetupActivity.java
            │   └── util/TimeUtil.java
            └── res/
                ├── drawable/
                │   ├── bg_gradient.xml
                │   ├── card_bg.xml
                │   ├── card_bg_high.xml
                │   ├── chip_bg.xml
                │   ├── ic_apps.xml
                │   ├── ic_check.xml
                │   ├── ic_clock.xml
                │   ├── ic_launcher_background.xml
                │   ├── ic_launcher_foreground.xml
                │   ├── ic_launcher_legacy.xml
                │   ├── ic_lock.xml
                │   ├── ic_moon.xml
                │   ├── ic_moon_small.xml
                │   └── ic_shield.xml
                ├── layout/
                │   ├── activity_apps_picker.xml
                │   ├── activity_locked.xml
                │   ├── activity_main.xml
                │   ├── activity_setup.xml
                │   ├── item_app.xml
                │   └── item_permission.xml
                ├── mipmap-anydpi/
                │   ├── ic_launcher.xml
                │   └── ic_launcher_round.xml
                ├── mipmap-anydpi-v26/
                │   ├── ic_launcher.xml
                │   └── ic_launcher_round.xml
                ├── values/
                │   ├── colors.xml
                │   ├── strings.xml
                │   └── themes.xml
                ├── values-ar/
                │   └── strings.xml
                └── xml/
                    ├── accessibility_config.xml
                    ├── backup_rules.xml
                    ├── data_extraction_rules.xml
                    └── device_admin.xml
```

ملاحظة: لا داعي لرفع `gradlew` و `gradle-wrapper.jar` — الـ GitHub Actions ينشئهما تلقائياً.

---

## 4) تشغيل البناء

1. اذهب لتبويب **Actions** في مستودعك
2. إذا طُلب منك تفعيل الـ Workflows، اضغط **I understand my workflows, go ahead and enable them**
3. سترى Workflow اسمه **Build APK** — اضغطه ثم اضغط **Run workflow → Run workflow**
4. انتظر 3-6 دقائق حتى تظهر علامة ✅ خضراء
5. ادخل على البناء الذي اكتمل، انزل للأسفل لقسم **Artifacts**
6. حمّل **AntiSahar-debug-apk** — هو ملف zip بداخله ملف APK
7. فُكَّ الـ zip، ستجد `AntiSahar-debug.apk`

---

## 5) تثبيت التطبيق على هاتفك

1. انقل ملف APK لهاتفك (واتساب، ميل، Google Drive...)
2. افتحه، إذا قال "تثبيت من مصادر غير معروفة" — فعّل الخيار
3. ثبّت التطبيق

---

## 6) أول مرة تفتح فيها التطبيق

1. اضغط **منح** بجانب كل صلاحية واقبل كل شيء:
   - **مدير الجهاز** (Device Admin) ← مهم جداً للحماية
   - **إمكانية الوصول** (Accessibility) ← مهم لقفل التطبيقات
   - **إحصائيات الاستخدام** (Usage Stats)
   - **العرض فوق التطبيقات** (Overlay)
   - **تجاهل تحسين البطارية** (Battery)
2. اضغط زر **خطّط لنومك**:
   - اضبط وقت السهر الحالي (مثلاً 03:30)
   - اضبط الوقت المستهدف (مثلاً 23:00)
   - حدد كم دقيقة سيتقدم كل يوم (افتراضياً 10 دقائق)
   - حدد مدة القفل بعد وقت النوم (افتراضياً 2 ساعة)
   - اضغط **حفظ الخطة**
3. اضغط **اختر التطبيقات المحظورة** وفعّل التطبيقات التي تريد قفلها (يوتيوب، تيك توك...)

---

## 7) كيف يعمل ويحميك

- كل 15 دقيقة، خدمة في الخلفية تحسب وقت النوم الذكي للّيلة (يقل تدريجياً بمقدار `step` كل يوم).
- عند حلول الوقت، يبدأ **نافذة القفل** لمدة الساعات التي حددتها.
- خلال النافذة:
  - إذا فتحت أي تطبيق مقفل → سترى شاشة القفل فوراً مع عدّاد تنازلي.
  - إذا حاولت دخول الإعدادات لحذف التطبيق أو إيقافه → يتم إرجاعك للشاشة الرئيسية تلقائياً.
  - زر "إعادة ضبط الخطة" داخل التطبيق يكون معطّلاً حتى انتهاء فترة القفل.

---

## 8) مشاكل شائعة

| المشكلة | الحل |
|---|---|
| لم يظهر `app-debug.apk` في Artifacts | افتح الـ workflow run وراجع الـ logs. تأكد أن جميع الملفات رُفعت بنفس البنية. |
| النظام يوقف التطبيق ليلاً | تأكد من منح "تجاهل تحسين البطارية" وإلغاء أي قاتل-تطبيقات (مثل في Xiaomi/Huawei). |
| لم تظهر التطبيقات في القائمة | بعض شركات الأجهزة تحجب `QUERY_ALL_PACKAGES` — أعد تشغيل الجهاز بعد التثبيت. |
| Accessibility يتوقف من تلقاء نفسه | في هواتف Xiaomi/Huawei: ادخل الإعدادات → التطبيقات → مضاد السهر → فعّل "تشغيل تلقائي" و"تشغيل في الخلفية". |

---

🌙 نوماً هانئاً!
