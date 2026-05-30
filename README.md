# 🌙 Anti-Sahar (مضاد السهر)

تطبيق أندرويد ذكي يساعدك على التغلب على السهر تدريجياً، عن طريق تقديم وقت النوم كل يوم بضع دقائق حتى تصل للوقت الطبيعي الذي تريده، مع قفل التطبيقات المُلهية في وقت النوم وحماية نفسه من الحذف خلال فترة القفل.

---

## ✨ المميزات

- ⏰ **تخفيض تدريجي ذكي**: تحدد وقت سهرك الحالي والوقت المستهدف، والتطبيق ينزل تلقائياً كل يوم بمقدار صغير (افتراضياً 10 دقائق) حتى تصل للهدف بدون أن تشعر.
- 📱 **قفل التطبيقات المُلهية**: اختر التطبيقات (يوتيوب، تيك توك، انستقرام...) التي تريد قفلها أثناء فترة النوم لمدة تحددها أنت.
- 🛡️ **حماية ذكية للتطبيق نفسه**: عن طريق Device Admin + Accessibility Service، لا يمكنك حذف أو إيقاف التطبيق خلال فترة القفل.
- 🌐 **عربي + إنجليزي** مع دعم كامل لـ RTL.
- 🎨 **واجهة عصرية أنيقة** بألوان داكنة هادئة (Material 3) — لا صور، لا أيقونات خارجية، كل شيء Vector داخلي حتى لا تحدث مشاكل أثناء البناء.

---

## 🛠️ كيف تبني APK من GitHub (بدون أي برنامج على جهازك)

1. أنشئ مستودع جديد على GitHub باسم `AntiSahar` (أو أي اسم).
2. ارفع جميع الملفات والمجلدات الموجودة هنا بنفس البنية تماماً.
3. اذهب إلى تبويب **Actions** في المستودع، فعّل الـ Workflows إذا طُلب منك.
4. كل push على فرع `main` سيُشغّل البناء تلقائياً. أو شغّله يدوياً من **Actions → Build APK → Run workflow**.
5. عند انتهاء البناء (~5 دقائق)، حمّل ملف `app-debug.apk` من قسم **Artifacts** أسفل الـ Workflow run.
6. انقله إلى هاتفك وثبّته (فعّل "تثبيت من مصادر مجهولة").

---

## 📂 بنية المشروع (لتعرف ماذا تنشئ وأين)

```
AntiSahar/
├── README.md
├── .gitignore
├── build.gradle                 ← مستوى المشروع
├── settings.gradle
├── gradle.properties
├── gradlew                      ← (يُنشأ تلقائياً، انظر التعليمات بالأسفل)
├── gradle/wrapper/
│   ├── gradle-wrapper.properties
│   └── gradle-wrapper.jar       ← (يُنشأ تلقائياً)
├── .github/workflows/
│   └── build.yml                ← يبني APK تلقائياً
└── app/
    ├── build.gradle             ← مستوى التطبيق
    ├── proguard-rules.pro
    └── src/main/
        ├── AndroidManifest.xml
        ├── java/com/antisahar/app/
        │   ├── MainActivity.java
        │   ├── ui/SetupActivity.java
        │   ├── ui/AppsPickerActivity.java
        │   ├── ui/LockedScreenActivity.java
        │   ├── service/LockAccessibilityService.java
        │   ├── service/DailyScheduleWorker.java
        │   ├── receiver/AdminReceiver.java
        │   ├── receiver/BootReceiver.java
        │   ├── data/Prefs.java
        │   └── util/TimeUtil.java
        └── res/
            ├── values/{strings,colors,themes}.xml
            ├── values-ar/strings.xml
            ├── layout/*.xml
            ├── drawable/*.xml
            ├── xml/{device_admin,accessibility_config,backup_rules,data_extraction_rules}.xml
            └── mipmap-anydpi-v26/ic_launcher.xml
```

---

## ⚠️ ملاحظة عن `gradlew` و `gradle-wrapper.jar`

لا يمكن نسخ هذين الملفين كنص (الأول سكربت تنفيذي والثاني ملف ثنائي). الـ **GitHub Actions workflow** سينشئهما تلقائياً عبر الأمر `gradle wrapper` قبل البناء — لا تقلق، ليس عليك فعل أي شيء يدوياً.

---

## 🔐 الصلاحيات المطلوبة بعد التثبيت

عند فتح التطبيق لأول مرة، سيطلب منك:
1. **Device Admin**: لمنع حذف التطبيق خلال فترة القفل.
2. **Accessibility Service**: لمنع فتح إعدادات النظام وحذف التطبيق.
3. **Usage Stats**: لمعرفة أي تطبيق مفتوح حالياً وقفله.
4. **Overlay (الرسم فوق التطبيقات)**: لعرض شاشة القفل فوق التطبيقات المحظورة.
5. **Battery Optimization Ignore**: حتى لا يوقفه النظام أثناء النوم.

---

## 📜 الترخيص

MIT — استخدمه وعدّله كما تشاء.
