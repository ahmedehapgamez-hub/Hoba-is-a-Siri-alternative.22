// جسر التواصل بين واجهة الويب وخدمة الخلفية الأصلية في أندرويد (Capacitor / Android Bridge)
let flashOn = false;
let bgRunning = false;

function addMessage(text, sender = 'assistant') {
  const box = document.getElementById('chatBox');
  const div = document.createElement('div');
  div.className = `msg ${sender}`;
  div.textContent = text;
  box.appendChild(div);
  box.scrollTop = box.scrollHeight;
}

function speak(text) {
  addMessage(text, 'assistant');
  if ('speechSynthesis' in window) {
    window.speechSynthesis.cancel();
    const utter = new SpeechSynthesisUtterance(text);
    utter.lang = 'ar-SA';
    utter.rate = 1.02;
    window.speechSynthesis.speak(utter);
  }
}

function getNativeBridge() {
  return window.NoraNativeBridge || null;
}

function toggleBackgroundService(enable) {
  const native = getNativeBridge();
  if (native && typeof native.startBackgroundAssistant === 'function' && enable) {
    native.startBackgroundAssistant();
  } else if (native && typeof native.stopBackgroundAssistant === 'function' && !enable) {
    native.stopBackgroundAssistant();
  }

  bgRunning = enable;
  document.getElementById('startBgBtn').style.display = enable ? 'none' : 'inline-block';
  document.getElementById('stopBgBtn').style.display = enable ? 'inline-block' : 'none';
  document.getElementById('modeBadge').textContent = enable ? 'نشط في الخلفية' : 'وضع التطبيق';
  document.getElementById('substatus').textContent = enable
    ? 'تعمل في خدمة أمامية وتستمع لكلمة «يا نورا»'
    : 'جاهزة للاستماع والأوامر';

  if (enable) {
    speak('تم تشغيل خدمة نورا في الخلفية. سأبقى متاحة في شريط الإشعارات.');
  } else {
    speak('تم إيقاف خدمة الخلفية.');
  }
}

function toggleFlash() {
  flashOn = !flashOn;
  const native = getNativeBridge();
  if (native && typeof native.setTorch === 'function') {
    native.setTorch(flashOn);
  }
  const tile = document.getElementById('tileFlash');
  tile.classList.toggle('active', flashOn);
  speak(flashOn ? 'تم تشغيل الفلاش.' : 'تم إطفاء الفلاش.');
}

function openSystemPanel(panel) {
  const native = getNativeBridge();
  const map = {
    wifi: { action: 'android.settings.WIFI_SETTINGS', label: 'إعدادات الواي فاي' },
    bluetooth: { action: 'android.settings.BLUETOOTH_SETTINGS', label: 'إعدادات البلوتوث' }
  };
  const target = map[panel];
  if (!target) return;
  if (native && typeof native.openSettings === 'function') {
    native.openSettings(target.action);
  }
  speak(`جاري فتح ${target.label}.`);
}

function startVoiceRecognition() {
  const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
  const orb = document.getElementById('orb');
  if (!SpeechRecognition) {
    speak('التعرف الصوتي المباشر غير مدعوم هنا، يمكنك كتابة الأمر أو استخدام خدمة أندرويد الصوتية.');
    return;
  }
  const rec = new SpeechRecognition();
  rec.lang = 'ar-SA';
  rec.interimResults = false;
  orb.classList.add('listening');
  document.getElementById('substatus').textContent = 'جاري الاستماع...';

  rec.onresult = (event) => {
    const transcript = event.results[0][0].transcript;
    handleCommand(transcript);
  };
  rec.onerror = () => {
    speak('لم أتمكن من التقاط الصوت بوضوح، حاول مرة أخرى.');
  };
  rec.onend = () => {
    orb.classList.remove('listening');
    document.getElementById('substatus').textContent = bgRunning
      ? 'تعمل في خدمة أمامية وتستمع لكلمة «يا نورا»'
      : 'جاهزة للاستماع والأوامر';
  };
  rec.start();
}

function handleCommand(rawText) {
  const inputEl = document.getElementById('cmdInput');
  const cmd = (rawText || '').trim();
  if (!cmd) return;
  addMessage(cmd, 'user');
  inputEl.value = '';

  const normalized = cmd.replace(/^يا\s*نورا\s*[،,]?\s*/i, '').trim();
  const native = getNativeBridge();

  if (normalized.includes('فلاش') || normalized.includes('كشاف')) {
    toggleFlash();
    return;
  }
  if (normalized.includes('واي فاي') || normalized.includes('وايفي')) {
    openSystemPanel('wifi');
    return;
  }
  if (normalized.includes('بلوتوث')) {
    openSystemPanel('bluetooth');
    return;
  }
  if (normalized.includes('يوتيوب')) {
    speak('جاري فتح يوتيوب.');
    if (native && typeof native.launchApp === 'function') {
      native.launchApp('com.google.android.youtube');
    } else {
      window.open('https://www.youtube.com', '_blank');
    }
    return;
  }
  if (normalized.includes('واتساب') || normalized.includes('واتس')) {
    speak('جاري فتح واتساب.');
    if (native && typeof native.launchApp === 'function') {
      native.launchApp('com.whatsapp');
    } else {
      window.open('https://wa.me/', '_blank');
    }
    return;
  }
  if (normalized.includes('الساعة') || normalized.includes('الوقت')) {
    const now = new Date().toLocaleTimeString('ar-EG', { hour: '2-digit', minute: '2-digit' });
    speak(`الساعة الآن ${now}.`);
    return;
  }
  if (normalized.includes('خلفية')) {
    toggleBackgroundService(!normalized.includes('إيقاف'));
    return;
  }

  speak(`تم استلام أمرك: «${normalized || cmd}». في النسخة المجمّعة على أندرويد يتم تمريره مباشرة إلى خدمة الخلفية.`);
}

// استقبال الأحداث القادمة من خدمة الخلفية في أندرويد (مثل كلمة التنبيه يا نورا)
window.onNativeWakeWordDetected = function(phrase) {
  addMessage(`تم رصد كلمة التنبيه: "${phrase}" من خدمة الخلفية`, 'assistant');
  startVoiceRecognition();
};
