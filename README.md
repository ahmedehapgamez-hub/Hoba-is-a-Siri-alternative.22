# حزمة مشروع مساعد «نورا» للعمل في خلفية أندرويد (Capacitor + Android Foreground Service)

تحتوي هذه الحزمة على واجهة الويب وجسر الربط مع خدمة الخلفية الأصلية في أندرويد (`Foreground Service`) حتى تبقى «نورا» حاضرة في شريط الإشعارات وتستمع لكلمة النداء «يا نورا».

## محتويات المشروع
- `www/index.html` و `www/js/nora-bridge.js`: واجهة المساعد الصوتي وربط الأزرار والأوامر بالجسر الأصلي.
- `package.json` و `capacitor.config.json`: إعدادات تحويل الموقع إلى تطبيق أندرويد عبر Capacitor.
- `android-native/AndroidManifest.xml`: صلاحيات الميكروفون، الخدمة الأمامية (`FOREGROUND_SERVICE_MICROPHONE`)، الكاميرا، والإشعارات.
- `android-native/NoraBackgroundService.java`: خدمة خلفية تعمل بإشعار مستمر وتستمع لكلمة «يا نورا» ثم تفتح التطبيق وتنبه الواجهة.
- `android-native/MainActivity.java`: الجسر البرمجي (`JavascriptInterface`) للتحكم الحقيقي في الفلاش، فتح الإعدادات، وتشغيل التطبيقات.

## خطوات التحويل وبناء ملف APK
1. ثبّت حزم Node.js:
   ```bash
   npm install
   ```
2. أنشئ منصة أندرويد وزامن الملفات:
   ```bash
   npm run cap:add:android
   npm run cap:sync
   ```
3. انسخ ملفات `android-native/AndroidManifest.xml`، `NoraBackgroundService.java`، و `MainActivity.java` إلى مسار مشروع أندرويد الناتج داخل `android/app/src/main/...` (مع مطابقة اسم الحزمة `com.nora.assistant`).
4. افتح المشروع في **Android Studio** عبر `npm run cap:open` ثم اختر **Build > Build Bundle(s) / APK(s) > Build APK(s)**.

> **ملاحظة تقنية مهمة:** في أندرويد الحديث، الاستماع المستمر عبر `SpeechRecognizer` في الخلفية قد يتوقف مؤقتاً أو يتأثر بقيود توفير البطارية؛ وللاستقرار العالي على مدار اليوم يُنصح بدمج محرك كشف كلمة التنبيه المحلي (مثل Porcupine) واستثناء التطبيق من تحسين البطارية (Battery Optimization).
