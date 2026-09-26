#!/usr/bin/env bash
# يثبّت نسخة المطوّر على المحاكي ويلتقط كل الشاشات (فاتح + داكن).
set -euo pipefail
APK="$1"; OUT="$2"
mkdir -p "$OUT"
adb install -r "$APK"
# نمنح إذن التنبيهات مسبقاً حتى نافذة الإذن ما تغطي اللقطات
adb shell pm grant app.sanad.coach android.permission.POST_NOTIFICATIONS || true
PKG=app.sanad.coach/.MainActivity

shot() {
  local name="$1"; local wait="$2"; shift 2
  adb shell am start -W -S -n "$PKG" "$@" > /dev/null
  sleep "$wait"
  adb exec-out screencap -p > "$OUT/$name.png"
  echo "captured $name"
}

scroll_shot() {
  local name="$1"
  adb shell input swipe 540 1900 540 700 400
  sleep 2.5
  adb exec-out screencap -p > "$OUT/$name.png"
  echo "captured $name"
}


# يضغط على عنصر نصه أو وصفه يطابق $1 (من شجرة الوصول)؛ ما يفشل السكربت إذا ما لقاه
tap_text() {
  adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 || true
  local b
  b=$(adb shell cat /sdcard/ui.xml | tr '>' '\n' | grep -F "\"$1\"" | head -1 | grep -o 'bounds="[^"]*"' | head -1 || true)
  local n=($(echo "$b" | grep -o '[0-9]\+' | tr '\n' ' '))
  if [ ${#n[@]} -lt 4 ]; then echo "AUDIT tap_text: NOT FOUND '$1'"; return 0; fi
  adb shell input tap $(( (n[0]+n[2])/2 )) $(( (n[1]+n[3])/2 ))
  echo "AUDIT tap_text: tapped '$1'"
}

# يمرّر لتحت لحد ما يبين النص $1، بعدين يلتقط $2
scroll_to() {
  local found=0
  for i in 1 2 3 4 5 6 7 8 9; do
    adb shell uiautomator dump /sdcard/ui.xml > /dev/null 2>&1 || true
    if adb shell cat /sdcard/ui.xml | grep -qF "$1"; then found=1; break; fi
    adb shell input swipe 540 1700 540 900 500
    sleep 1.5
  done
  if [ $found = 1 ]; then
    adb shell input swipe 540 1500 540 1100 400
    echo "AUDIT scroll_to: found '$1'"
  else
    echo "AUDIT scroll_to: NOT FOUND '$1'"
  fi
  sleep 1.5
  adb exec-out screencap -p > "$OUT/$2.png"
  echo "captured $2"
}

cap() { sleep "${2:-1.5}"; adb exec-out screencap -p > "$OUT/$1.png"; echo "captured $1"; }

adb shell cmd uimode night no
shot 00-intro 2.2 --ez fresh true
shot 01-welcome 3 --ez fresh true
shot 02-today-checkin 7 --ez demo true --ez skipIntro true
scroll_shot 03-today-checkin-scrolled
shot 04-today-plan 7 --ez demo true --ez skipIntro true --es checkin HIGH
scroll_shot 05-today-plan-scrolled
scroll_shot 06-today-plan-bottom
shot 07-eat 7 --ez demo true --ez skipIntro true --es route eat
shot 15-today-tired 7 --ez demo true --ez skipIntro true --es checkin LOW
scroll_shot 16-today-tired-scrolled
scroll_shot 17-today-tired-bottom
shot 19-today-gathering 7 --ez demo true --ez skipIntro true --es checkin MID --ez gathering true
scroll_shot 19b-today-gathering-scrolled
shot 08-move 7 --ez skipIntro true --es route move
scroll_shot 09-move-library
shot 10-exercise 7 --ez skipIntro true --es route exercise/squat
shot 11-player 8 --ez skipIntro true --es route player/strength-10
shot 12-coach 7 --ez skipIntro true --es route coach
shot 24-pacer 9 --ez skipIntro true --es route pacer
shot 25-plate 7 --ez skipIntro true --es route plate
shot 13-progress 7 --ez demo true --ez skipIntro true --es route progress
scroll_shot 14-progress-scrolled
scroll_shot 18-progress-bottom


# ---------------- تدقيق الإضافات (كل ميزة لازم تبين بلقطة) ----------------
shot 40-welcome-back 8 --ez skipIntro true --ez away true
shot 41-radar-tired 8 --ez demo true --ez skipIntro true --es checkin LOW --es sleep 5
scroll_to "صار شي اليوم؟" 42-lapse-card
tap_text "زلّيت"; cap 43-lapse-open 2
tap_text "أكلت هواية"; cap 44-lapse-recovery 2
scroll_to "سجّلها وكمّل يومي" 44b-lapse-recovery-steps
tap_text "سجّلها وكمّل يومي"; cap 45-lapse-logged 2

shot 46-today-lesson 8 --ez demo true --ez skipIntro true --es checkin MID
scroll_to "قريته، أجرّبها اليوم" 47-lesson-card
tap_text "قريته، أجرّبها اليوم"; cap 48-lesson-read 2

shot 49-progress-audit 8 --ez demo true --ez skipIntro true --es route progress
scroll_to "طقس الميزان" 50-weigh-in-weather
scroll_to "آخذ إبر أو حبوب التنحيف" 51-glp1-off
tap_text "وضع أدوية التنحيف"; cap 52-glp1-on 2
shot 53-today-glp1-plan 8 --ez skipIntro true
scroll_to "البروتين أول لقمة" 54-today-glp1-missions

shot 55-progress-partner 8 --ez demo true --ez skipIntro true --ez partner true --es route progress
scroll_to "ارسل تقرير الأسبوع" 56-partner-set
tap_text "ارسل تقرير الأسبوع لـحسن"; cap 57-partner-share-sheet 3
adb shell input keyevent KEYCODE_BACK; sleep 1

shot 58-goal-reached 8 --ez skipIntro true --ez reached true --es route progress
scroll_to "وصلت هدفك" 59-goal-reached-card
tap_text "ابدأ وضع الحفاظ"; sleep 2
scroll_to "وضع الحفاظ" 60-maintenance

shot 61-coach-settings 8 --ez skipIntro true --es route coach-settings

# التنبيه: نعرض التنبيه الجاي فوراً ونفتح لوحة الإشعارات
shot 62-notify 6 --ez demo true --ez skipIntro true --ez notifyNow true
adb shell cmd statusbar expand-notifications; cap 63-notification-shade 2
adb shell cmd statusbar collapse
echo "AUDIT alarms:"; adb shell dumpsys alarm | grep -A2 "app.sanad.coach" | head -12 || true
echo "AUDIT step sensor:"; adb shell dumpsys sensorservice | grep -i "step" | head -5 || true

adb shell cmd uimode night yes
shot 20-dark-today 7 --ez demo true --ez skipIntro true --es checkin MID
shot 21-dark-move 7 --ez skipIntro true --es route move
shot 22-dark-player 8 --ez skipIntro true --es route player/low-impact-10
shot 23-dark-progress 7 --ez skipIntro true --es route progress
adb shell cmd uimode night no

# وضع رمضان (آخر شي لأنه يبقى مفعّل)
shot 30-ramadan-today 7 --ez demo true --ez skipIntro true --es checkin HIGH --ez ramadan true
scroll_shot 31-ramadan-today-scrolled

# أي انهيار؟
if adb logcat -d | grep -E "FATAL EXCEPTION|AndroidRuntime: Process: app.sanad.coach" ; then
  adb logcat -d | grep -A 30 "FATAL EXCEPTION" > "$OUT/crash.txt" || true
  echo "::error::App crashed during screenshots"
  exit 1
fi
