# KinderGate MVP – Kompletna dokumentacja architektoniczna

> Wersja: 0.1.0-mvp · Data: 2026-03 · Autor: Claude (senior Android engineer perspective)

---

## 1. Executive Summary

### Najważniejsza decyzja architektoniczna

KinderGate MVP działa jako **Foreground Service** z hybrydowym wykrywaniem foreground app (**AccessibilityService jako główna metoda + UsageStatsManager jako fallback**) i blokowaniem przez **pełnoekranową Activity**. AccessibilityService daje natychmiastową detekcję; UsageStats (polling 1s) zapewnia fallback na urządzeniach, gdzie Accessibility jest niedostępna lub wyłączona.

### Trzy fundamentalne kompromisy MVP

| Kompromis | Decyzja | Dlaczego |
|-----------|---------|----------|
| Overlay vs BlockingActivity | BlockingActivity | Nie wymaga SYSTEM_ALERT_WINDOW jako głównego mechanizmu; mniej ryzykowne w Play Store |
| AccessibilityService primary vs secondary | **Primary** (z fallbackiem UsageStats) | Daje natychmiastową reakcję bez opóźnienia 1s; UsageStats jako backup gdy Accessibility niedostępna |
| Device Admin vs without | Without (opt-in warning) | Device Admin to enterprise API; Play Store nie lubi go w consumer apps |

### Co to MVP potrafi

- Wykrywać foreground app z 1s opóźnieniem
- Liczyć aktywny czas używając monotonnego zegara (immuny na zmianę czasu)
- Wyświetlić ekran PAUZA po 60s (konfigurowalny)
- Przeżyć restart urządzenia
- Wykrywać sabotaż uprawnień i informować rodzica
- Działać jako transparentne, zgodne z polityką Google narzędzie

### Co MVP NIE potrafi (i nie obiecuje)

- Zapobiec wyłączeniu przez zaawansowanego użytkownika z dostępem do ustawień
- Działać w safe mode
- Przeżyć force stop bez wsparcia Device Admin
- Gwarantować 100% pokrycia na MIUI/EMUI bez ręcznej konfiguracji

---

## 2. Realistyczne ograniczenia Androida

### Komunikat dla rodzica (UI copy)

```
KinderGate działa transparentnie i niezawodnie na większości urządzeń,
ale Android – jako system otwarty – pozwala użytkownikowi z dostępem
do ustawień na wyłączenie dowolnej aplikacji.

Tego nie możemy zagwarantować:
• Działanie w trybie awaryjnym (safe mode) – Android wyłącza wszystkie
  aplikacje firm trzecich. Poinformuj dziecko, że restartowanie w safe mode
  jest zauważalne i rejestrowane.
• Absolutna nieusuwalność – każdą aplikację można odinstalować w ustawieniach.
  KinderGate utrudnia to, ale nie może zablokować całkowicie bez MDM.
• Działanie po force stop – jeśli dziecko wyłączy KinderGate przez
  Ustawienia > Aplikacje > Wymuś zatrzymanie, ochrona zostaje wyłączona
  do następnego restartu. Dostaniesz o tym powiadomienie.

Co robimy zamiast tego:
• Wykrywamy sabotaż i natychmiast powiadamiamy Cię
• Przywracamy działanie po restarcie automatycznie
• Zwiększamy koszt manipulacji, by nie opłacało się jej próbować
```

### Tabela ograniczeń technicznych

| Scenariusz | Skutek | Mitigacja | Gwarancja |
|------------|--------|-----------|-----------|
| Safe mode | Usługa nie działa | Brak (limitation of platform) | Brak |
| Force stop | Usługa zatrzymana do restartu | TamperEvent + reboot recovery | Częściowa |
| Revoke UsageStats | Detekcja niemożliwa | Alert rodzica + fallback na Accessibility | Fallback |
| Factory reset | Pełne usunięcie | Brak | Brak |
| Drugie konto na telefonie | Nie monitorujemy | Brak w MVP | Brak |
| VPN/proxy | Nie blokujemy sieci | Brak w MVP | Brak |
| Drugi telefon | Nie monitorujemy | Brak w MVP | Brak |

---

## 3. Architektura systemu

### Diagram komponentów

```
┌────────────────────────────────────────────────────────────────┐
│                        PARENT UI LAYER                          │
│  MainActivity → NavGraph → OnboardingScreen → DashboardScreen  │
│                            AppPickerScreen                      │
└────────────────────────┬───────────────────────────────────────┘
                         │ ViewModels (Hilt, StateFlow)
┌────────────────────────▼───────────────────────────────────────┐
│                       DOMAIN LAYER                              │
│  MonitoredAppsRepository  SessionRepository  ConfigRepository   │
│  GetInstalledAppsUseCase  ManageMonitoredAppsUseCase            │
└──────────┬──────────────────┬──────────────────────────────────┘
           │                  │
┌──────────▼──────┐  ┌────────▼────────────────────────────────┐
│   DATA LAYER    │  │           SERVICE LAYER                  │
│  Room Database  │  │  MonitorService (ForegroundService)       │
│  EncryptedPrefs │  │    ├─ AppUsageDetector                   │
│  RepoImpls      │  │    │    ├─ AccessibilityService (priority 1)│
└─────────────────┘  │    │    ├─ UsageEvents/10s (priority 2)    │
                     │    │    └─ UsageStats aggregated (priority 3)│
                     │    ├─ SessionTimer (elapsedRealtime)         │
                     │    └─ TamperDetector (health check co 30s)   │
                     │                                           │
                     │  BlockingActivity (separate task)         │
                     └─────────────────────────────────────────┘
                               │ Receivers
                     ┌─────────▼──────────────────────────────┐
                     │  BootReceiver  PackageEventReceiver     │
                     └────────────────────────────────────────┘
```

### Przepływ danych: wykrycie blokady

```
KinderGateAccessibilityService.onAccessibilityEvent()
  │  [TYPE_WINDOW_STATE_CHANGED – natychmiast]
  ├─ lastForegroundPackage.set(pkg)         ← AtomicReference odczytany przez AppUsageDetector
  └─ packageEvents.tryEmit(pkg)             ← SharedFlow; MonitorService.observePackageEvents()
       └─ MonitorService.tick(isEventTriggered=true)  ← natychmiastowy tick bez czekania na polling

MonitorService.tick() [polling co 1s + event-triggered]
  │
  ├─ AppUsageDetector.getForegroundPackage()   [hybrydowe, 3-poziomowe]
  │    ├─ [priority 1] KinderGateAccessibilityService.lastForegroundPackage.get()
  │    ├─ [priority 2] UsageStatsManager.queryEvents(now-10s, now)
  │    │    → ACTIVITY_RESUMED events → most recent package
  │    └─ [priority 3] UsageStatsManager.queryUsageStats(INTERVAL_DAILY, now-1h, now)
  │         → maxByOrNull { lastTimeUsed }
  │
  ├─ package in monitoredPackages?
  │    NO → SessionTimer.onNonMonitoredForeground() → timer pauses/resets
  │    YES →
  │         SessionTimer.tick(package, intervalMs)
  │              uses SystemClock.elapsedRealtime() [monotonic, manipulation-immune]
  │              returns shouldBlock: Boolean
  │
  └─ shouldBlock?
       NO → continue polling
       YES →
            SessionRepository.insertBlockSession(...)
            MonitorService.launchBlockingScreen(packageName)
                 Intent(BlockingActivity) with FLAG_ACTIVITY_NEW_TASK
                 → BlockingActivity comes to foreground
                      → child sees PAUSE screen
                      → child presses OK
                           → MonitorService.ACTION_BLOCK_ACKNOWLEDGED
                                → SessionTimer.onBlockAcknowledged()
                                → SessionRepository.acknowledgeSession()
                                → timer resets to IDLE
```

---

## 4. Komponenty Android

### MonitorService
- `Service` subclass, `foregroundServiceType="specialUse"`
- Trzyma `PARTIAL_WAKE_LOCK` (timeout 1h) aby CPU nie zasypiał
- `START_STICKY` – Android restartuje service po kill
- **Dwa źródła ticków:**
  1. Coroutine polling loop co 1s (`monitorJob`)
  2. Event-driven tick z `KinderGateAccessibilityService.packageEvents` SharedFlow (`eventJob`) – reaguje natychmiastowo na zmianę okna
- Drugi loop co 30s sprawdza uprawnienia (tamper detection, `healthCheckJob`)
- `observeMonitoredApps()` – reaktywnie aktualizuje listę `excludedPackages` z Room DB

### AccessibilityService
- Minimalna konfiguracja: tylko `TYPE_WINDOW_STATE_CHANGED`, `notificationTimeout=100ms`
- `canRetrieveWindowContent = false` – nie czytamy treści aplikacji
- Utrzymuje `AtomicReference<String?>` z ostatnim foreground package (`lastForegroundPackage`)
- Emituje `packageEvents: MutableSharedFlow<String>` – natychmiastowy trigger dla `MonitorService`
- **Główna metoda detekcji (priority 1)** w `AppUsageDetector`; UsageStats jako backup gdy Accessibility jest wyłączona
- `isServiceConnected: Boolean` – dostępny stan połączenia dla diagnostyki

### BlockingActivity
- `android:taskAffinity="pl.kindergate.blocking"` – oddzielny task
- `android:launchMode="singleInstance"` – jeden egzemplarz
- `android:excludeFromRecents="true"` – nie pojawia się w recent apps
- `FLAG_SHOW_WHEN_LOCKED | FLAG_KEEP_SCREEN_ON | FLAG_TURN_SCREEN_ON`
- Override `onBackPressed()` – back button nic nie robi
- `setShowWhenLocked(true)` API 27+

### BootReceiver
- Nasłuchuje `BOOT_COMPLETED` + vendor-specific equivalents (QUICKBOOT_POWERON dla HTC, MIUI Resume)
- Sprawdza `SecurePrefs.isOnboardingComplete()` i `isMonitoringEnabled()` przed startem
- Heurystyka force stop: jeśli `lastKnownServiceUptimeMs != 0` (poprzedni run odnotowany), loguje `TamperType.BOOT_AFTER_SHORT_UPTIME` z zarejestrowanym uptime; informacja dla rodzica w DashboardScreen
- Uruchamia `MonitorService` przez `startForegroundService()` (API 26+)

### Room Database
- `MonitoredApp`, `BlockSession`, `TamperEvent` entities
- Retention: 30 dni (cleanup job w v1)
- `exportSchema = true` dla migrations tracking

### EncryptedSharedPreferences
- Backed by Android Keystore
- Przechowuje: PIN hash (SHA-256), monitoring enabled flag, block interval
- Wrażliwy plik wykluczony z cloud backup

---

## 5. Strategia odporności na obejścia

| Typ obejścia | Ryzyko | Detekcja | Reakcja | Co NIE da się zatrzymać |
|-------------|--------|----------|---------|------------------------|
| **Restart telefonu** | Wysoki | BootReceiver | Automatyczny restart usługi | Safe mode wyłącza receiver |
| **Force stop** | Wysoki | Brak real-time; heurystyka przy restarcie | TamperEvent + powiadomienie rodzica | Usługa zatrzymana do restartu |
| **Wyłączenie accessibility** | Średni | Health check co 30s | TamperEvent; UsageStats polling jako fallback nadal działa | Degradacja z event-driven do 1s polling |
| **Revoke usage stats** | Wysoki | Health check co 30s | Alert dla rodzica; Accessibility nadal działa jako primary | Utrata fallbacku; monitoring działa jeśli Accessibility włączona |
| **Revoke overlay** | Niski | Health check | TamperEvent | BlockingActivity nadal działa bez overlay |
| **Battery optimization włączona** | Średni | PowerManager.isIgnoring... | TamperEvent; UI alert | OEM może ograniczyć service |
| **Zmiana czasu systemowego** | Niski | N/A (timer immunny) | Brak potrzeby | Elapsedreality nie zmienia się |
| **Odinstalowanie** | Wysoki | PackageEventReceiver (tylko po fakcie) | TamperEvent w bazie | Nie da się zapobiec bez Device Admin |
| **Safe mode** | Wysoki | Brak (device reboots into safe mode) | Po wyjściu z safe mode: BootReceiver restartuje | Absolutnie nie da się zatrzymać |
| **Drugie konto** | Wysoki | Brak w MVP | Informacja rodzica w onboarding | Brak mechanizmu cross-account |
| **Wyczyszczenie danych** | Krytyczny | Brak (dane tracone) | Brak; rodzic musi reinstalować | Bez Device Admin nie da się chronić danych |
| **Szybkie przełączanie apps** | Niski | Timer resetuje się przy zmianie | Akceptowalna funkcjonalność | By design: timer jest per-session |
| **ADB shell stop** | Krytyczny | Brak | Brak | Root access = game over |

### Podejście do utrudnienia odinstalowania (legalne)

MVP nie używa Device Admin. Uzasadnienie:
1. Device Admin wymaga `android:exported="true"` na `DeviceAdminReceiver`, co Play Store może oznaczać jako ryzykowne
2. Play Store zaostrzył politykę wobec consumer apps używających Device Admin
3. Device Admin można odwołać przez Settings > Security > Device Admins – dziecko to może znaleźć

**Zamiast tego:**
- Wyraźna komunikacja rodzicowi: "To narzędzie transparentne, nie niemożliwe do usunięcia"
- W v1: opcjonalna warstwa Device Admin z jasnym onboardingiem
- W v2: rozważyć Android Enterprise (managed device) dla głębszej kontroli

---

## 6. UX rodzica – onboarding krok po kroku

### Krok 0: Ekran powitalny
- Wyjaśnienie czym jest KinderGate
- Jasne ostrzeżenie o transparentności (nie inwigilacja)
- Czas trwania onboardingu: ~3 minuty

### Krok 1: Uprawnienia (wymagane)
1. **Usage Stats Access** → Settings > Apps > Special app access > Usage access
   - Jedyne uprawnienie konieczne dla działania MVP
   - Przycisk "Udziel" otwiera systemowy ekran bezpośrednio
2. **Notifications** → system dialog (API 33+) lub auto-granted
3. **Draw over apps** → Settings > Apps > Special app access > Display over other apps
   - Wymagane dla fallback mechanizmu blokady
4. **Battery optimization** → Intent z direct exemption request

### Krok 2: Uprawnienia (zalecane)
5. **Accessibility Service** → Settings > Accessibility
   - Z pełnym wyjaśnieniem: "To zapasowy mechanizm detekcji; nie czytamy treści"
   - Możliwość pominięcia

### Krok 3: Autostart (dla Xiaomi/Huawei)
- Wykrywamy producenta przez `Build.MANUFACTURER`
- Dla Xiaomi: instrukcja "Security > Autostart > KinderGate"
- Dla Huawei: instrukcja "Phone Manager > App Launch > Manual"
- Link do FAQ z GIF-ami

### Krok 4: PIN rodzica
- 4-8 cyfr
- Hash SHA-256 przechowywany w EncryptedSharedPreferences
- Potwierdzenie PINu
- Ostrzeżenie: "Zapamiętaj PIN – bez niego nie możesz zmienić ustawień"

### Krok 5: Wybór aplikacji
- Lista zainstalowanych apps (bez systemowych)
- Domyślnie preoznaczone: TikTok, YouTube, Instagram, Snapchat, Gaming-category apps
- Możliwość pominięcia i wybrania później

### Krok 6: Panel zdrowia systemu
- Stały komponent w DashboardScreen
- Zielony / żółty / czerwony status
- Każdy brakujący element z przyciskiem "Napraw"

---

## 7. UX dziecka – ekran PAUSE

### Layout
```
┌─────────────────────────────────┐
│                                 │
│                                 │
│         [ ⏸ pulsujący ]         │
│        (animacja 1s, skala)     │
│                                 │
│      P A U Z A                  │
│    (64sp, ExtraBold, biały)     │
│                                 │
│     Czas na przerwę!            │
│   (18sp, amber/pomarańczowy)    │
│                                 │
│  Masz chwilę, żeby odpocząć     │
│         od ekranu.              │
│  (14sp, szary, 70% opacity)     │
│                                 │
│  ┌───────────────────────────┐  │
│  │    OK, rozumiem           │  │
│  └───────────────────────────┘  │
│  (amber button, 56dp, full-w)   │
│                                 │
└─────────────────────────────────┘
Tło: #0D1B2A (głęboki granat)
```

### Stany

| Stan | Co widzi dziecko | Co dzieje się w tle |
|------|-----------------|---------------------|
| Initial | Pełny ekran PAUZA | Timer w stanie BLOCKING |
| After OK | Ekran znika, monitored app wraca | Timer resetuje do IDLE |
| Child navigates home | Monitoring redetects monitored app, relaunches | Service re-triggers block |
| Child goes to non-monitored app | Block screen stays until they return or press OK | Timer stays BLOCKING |

### Edge cases

**Dziecko szybko przełącza aplikacje:**
- Timer resetuje się przy każdej zmianie na niemonitorowaną aplikację
- Jeśli dziecko wraca do tej samej monitorowanej aplikacji, okno 60s zaczyna od zera
- To jest celowe: nie chcemy karać za naturalne przełączanie między apkami

**Dziecko wróci do tej samej aplikacji od razu po OK:**
- Po OK timer wraca do IDLE
- Nowe 60s zaczyna się od momentu powrotu do aplikacji
- Dziecko dostaje pełne kolejne 60s

**Ekran blokujący przy słabym połączeniu (nie dotyczy MVP):**
- BlockingActivity nie wymaga sieci
- Wszystko lokalne

**Dostępność (accessibility):**
- Cały ekran ma `contentDescription` dla screen readerów
- Przycisk OK ma minimalny touch target 56dp height
- Kontrast tekstu na tle spełnia WCAG AA

---

## 8. Model domenowy

### Encje

```kotlin
MonitoredApp(
    packageName: String,      // PK
    appLabel: String,         // label z PackageManager
    isEnabled: Boolean,       // można tymczasowo wyłączyć bez usuwania
    addedAtMs: Long
)

BlockSession(
    id: Long,                            // auto-generated
    packageName: String,
    elapsedRealtimeTriggeredMs: Long,    // monotonic – używany do logiki
    wallClockTriggeredMs: Long,          // wall clock – tylko do wyświetlania
    acknowledgedAtElapsedMs: Long?,      // null = dziecko jeszcze nie kliknęło OK
    sessionDurationMs: Long              // ile czasu dziecko było w apce przed PAUSE
)

TamperEvent(
    id: Long,
    type: TamperType,          // enum: USAGE_STATS_REVOKED, FORCE_STOP_DETECTED, etc.
    detectedAtMs: Long,
    detail: String
)

AppConfig(                     // nie persystowana jako encja – prefs
    blockIntervalSeconds: Int, // default 60
    isMonitoringEnabled: Boolean,
    hasCompletedOnboarding: Boolean,
    pinConfigured: Boolean
)
```

### Zdarzenia domenowe (MVP jako sealed class)

```kotlin
sealed class MonitorEvent {
    data class AppForegrounded(val packageName: String) : MonitorEvent()
    data class TimerTickCompleted(val elapsedMs: Long) : MonitorEvent()
    data class BlockTriggered(val packageName: String, val sessionId: Long) : MonitorEvent()
    data class BlockAcknowledged(val sessionId: Long) : MonitorEvent()
    data class TamperDetected(val type: TamperType) : MonitorEvent()
}
```
*Uwaga: MonitorEvent jest przygotowaniem architektonicznym na v1 (event bus), w MVP zdarzenia przepływają przez StateFlow i Service intents.*

---

## 9. Persistence i bezpieczeństwo danych

### Co trzymamy i gdzie

| Dane | Storage | Szyfrowanie | Backup |
|------|---------|-------------|--------|
| PIN hash | EncryptedSharedPreferences | AES-256-GCM (Keystore) | NIE |
| Monitoring enabled | EncryptedSharedPreferences | AES-256-GCM | NIE |
| Block interval | EncryptedSharedPreferences | AES-256-GCM | TAK |
| Monitored apps | Room DB | SQLite (nie szyfrowany) | TAK |
| Block sessions (log) | Room DB | brak | NIE |
| Tamper events | Room DB | brak | NIE |

### PIN security

```
PIN (4-8 cyfr, plain text)
    │
    ▼
SHA-256(PIN) → 64 hex chars
    │
    ▼
EncryptedSharedPreferences
    │ (backed by Android Keystore AES-256-GCM)
    ▼
Filesystem: /data/data/pl.kindergate/shared_prefs/kindergate_secure.xml
```

**Uwaga dla v1:** SHA-256 jest wystarczający dla 4-8 cyfrowego PINu przy założeniu, że:
1. Plik jest szyfrowany przez Keystore (nie ma bezpośredniego dostępu bez root)
2. Nie ma rate limiting attacków offline (brak dostępu do pliku)
3. Na potrzeby v1 rozważyć Argon2 dla głębszej ochrony

### Recovery po restarcie procesu

`SecurePreferencesManager` przechowuje `lastKnownServiceUptimeMs` i `serviceStartedAtElapsed`.

Przy starcie `BootReceiver`:
1. Odczyt `lastKnownServiceUptimeMs` – jeśli bardzo krótki → `TamperType.BOOT_AFTER_SHORT_UPTIME`
2. `SessionTimer.reset()` – konserwatywnie czyścimy stan (bezpieczniejsze niż rekonstrukcja)
3. Nowe 60s okno dla każdej aplikacji

---

## 10. Plan implementacji

### Etap 1: Szkielet projektu
**Cele:** Buildable project, Hilt wired, Room working
**Output:** `app` module z Gradle config, DI, empty DB
**Ryzyka:** Kotlin/AGP version conflicts
**DoD:** `./gradlew assembleDebug` bez błędów

### Etap 2: Onboarding i uprawnienia
**Cele:** Rodzic może przejść przez 5-krokowy onboarding
**Output:** `OnboardingScreen`, permission checks, PIN setup
**Ryzyka:** EncryptedSharedPreferences init failure na rooted devices
**DoD:** Onboarding przejść na 3 różnych urządzeniach (Samsung, Xiaomi, Pixel)

### Etap 3: Detekcja foreground app
**Cele:** `AppUsageDetector` zwraca poprawny package co 1s
**Output:** `AppUsageDetector` + `KinderGateAccessibilityService`
**Ryzyka:** MIUI blokuje UsageStats mimo przyznanego uprawnienia
**DoD:** Detekcja działa na Pixel + Samsung + Xiaomi; fallback na Accessibility pokazuje poprawny package

### Etap 4: Licznik 60 sekund
**Cele:** `SessionTimer` liczy czas używając `elapsedRealtime()`
**Output:** `SessionTimer` + unit tests
**Ryzyka:** Race condition przy szybkim przełączaniu aplikacji
**DoD:** Wszystkie unit testy przechodzą; zmiana czasu systemowego nie wpływa na licznik

### Etap 5: Ekran PAUSE
**Cele:** `BlockingActivity` pojawia się po 60s i pozostaje widoczny
**Output:** `BlockingActivity` + `BlockingScreen`
**Ryzyka:** Activity nie pokazuje się nad lock screen
**DoD:** Block pojawia się na Pixel, Samsung (One UI), Xiaomi; back button nie zamyka; home button ponownie otwiera

### Etap 6: Restart i autostart
**Cele:** Monitoring działa po restarcie urządzenia
**Output:** `BootReceiver` + MonitorService autostart
**Ryzyka:** MIUI wymaga ręcznego włączenia autostartu; Huawei blokuje background process
**DoD:** Usługa wznawia monitoring po restarcie na 3 testowych urządzeniach (max 60s opóźnienia)

### Etap 7: Tamper detection
**Cele:** Rodzic dostaje powiadomienie gdy uprawnienia zostają odwołane
**Output:** Health check loop + TamperEvent DB + push notification
**Ryzyka:** False positives przy normalnym użytkowaniu
**DoD:** Test: revoke Usage Stats → powiadomienie w < 60s; force stop → TamperEvent przy następnym restarcie

### Etap 8: Testy i hardening
**Cele:** Stabilność na top 5 OEM; 0 crashes w 24h smoke test
**Output:** Unit tests pass; manual OEM checklist
**Ryzyka:** Samsung DeX mode edge cases; split-screen behavior
**DoD:** 24h smoke test na 3 urządzeniach bez crash; wszystkie unit testy zielone

---

## 11. Test plan

### Unit tests (JVM, bez urządzenia)

| Test | Plik | Co testuje |
|------|------|-----------|
| SessionTimer state machine | `SessionTimerTest.kt` | IDLE→RUNNING→BLOCKING transitions |
| Timer immune to clock change | `SessionTimerTest.kt` | `elapsedRealtime` mock |
| App switch resets timer | `SessionTimerTest.kt` | `onNonMonitoredForeground()` |
| ConfigRepository (PIN, interval) | `ConfigRepositoryTest.kt` | PIN hash verify, block interval, monitoring flag |
| MonitoredApps CRUD | `MonitoredAppsRepositoryTest.kt` | Add/remove/toggle |
| SimpleAdditionEvaluator | `task/SimpleAdditionEvaluatorTest.kt` | Correct/incorrect/trim/case answers |
| SimpleTaskEngine adaptive difficulty | `task/SimpleTaskEngineTest.kt` | Level up/down after window; getChildProgress |

### Integration tests (Device/Emulator)

| Test | Plik | Co testuje |
|------|------|-----------|
| Room insert/query | `AppDatabaseTest.kt` | DB integrity |
| Session acknowledge | `AppDatabaseTest.kt` | acknowledgedAtElapsedMs |
| Permission status | Manual | checkUsageStatsPermission() |

### Scenariusze obejścia (manual device tests)

| Scenariusz | Urządzenia | Oczekiwany wynik |
|-----------|------------|-----------------|
| Zmiana czasu o +1h | Pixel, Samsung | Timer nie wyzwala przedwcześnie |
| Home button na BlockingActivity | Wszystkie | Powrót do monitored app ponownie pokazuje blok |
| Recent apps → swipe off | Wszystkie | BlockingActivity nie pojawia się w recent apps |
| Revoke Usage Stats | Pixel | Alert dla rodzica w < 60s |
| Force stop | Pixel | TamperEvent po restarcie |
| Reboot | Pixel, Samsung, Xiaomi | Monitoring wznawia w < 60s |
| Safe mode reboot | Pixel | Monitoring nie działa; wznawia po normalnym restarcie |
| Install 2nd launcher | Samsung | Detekcja nadal działa |
| Split screen | Samsung, Pixel | Timer zachowuje się poprawnie (tylko top app) |
| Disable accessibility | Wszystkie | Fallback na UsageStats; TamperEvent |

### OEM variance matrix

| OEM/ROM | UsageStats | Accessibility | Background service | Autostart needed |
|---------|-----------|--------------|-------------------|-----------------|
| Pixel (stock) | ✓ | ✓ | ✓ | Nie |
| Samsung OneUI | ✓ | ✓ | Ograniczone (Deep Sleep) | Nie (battery whitelist) |
| Xiaomi MIUI | Częściowo | ✓ | Agresywne ograniczenia | TAK (Security app) |
| Huawei EMUI | Częściowo | ✓ | Agresywne ograniczenia | TAK (Phone Manager) |
| Oppo ColorOS | ✓ | ✓ | Ograniczone | Tak (w ustawieniach app) |
| Sony Xperia | ✓ | ✓ | ✓ | Nie |

---

## 12. Struktura projektu

```
kinder-gate/
├── settings.gradle.kts
├── build.gradle.kts
├── gradle/
│   └── libs.versions.toml
└── app/
    ├── build.gradle.kts
    ├── proguard-rules.pro
    └── src/
        ├── main/
        │   ├── AndroidManifest.xml
        │   ├── res/
        │   │   ├── drawable/
        │   │   │   ├── ic_shield.xml
        │   │   │   └── ic_warning.xml
        │   │   ├── mipmap-anydpi-v26/
        │   │   │   ├── ic_launcher.xml
        │   │   │   └── ic_launcher_round.xml
        │   │   ├── values/
        │   │   │   ├── strings.xml
        │   │   │   ├── colors.xml
        │   │   │   └── themes.xml
        │   │   └── xml/
        │   │       ├── accessibility_service_config.xml
        │   │       ├── backup_rules.xml
        │   │       └── data_extraction_rules.xml
        │   └── java/pl/kindergate/
        │       ├── KinderGateApplication.kt
        │       ├── MainActivity.kt
        │       │
        │       ├── domain/
        │       │   ├── model/
        │       │   │   ├── AppConfig.kt
        │       │   │   ├── BlockSession.kt
        │       │   │   ├── InstalledApp.kt
        │       │   │   ├── MonitoredApp.kt
        │       │   │   ├── PermissionStatus.kt
        │       │   │   ├── TamperEvent.kt         ← includes MonitorState
        │       │   │   └── task/                  ← Task Engine subdomain models
        │       │   │       ├── ChildProgress.kt
        │       │   │       ├── CognitiveSkill.kt
        │       │   │       ├── DifficultyMode.kt
        │       │   │       ├── EvaluationResult.kt
        │       │   │       ├── Task.kt
        │       │   │       ├── TaskContent.kt     ← sealed class (SimpleAdditionContent…)
        │       │   │       ├── TaskContext.kt
        │       │   │       ├── TaskMetadata.kt
        │       │   │       ├── TaskSet.kt
        │       │   │       ├── TaskSource.kt
        │       │   │       ├── TaskSubject.kt
        │       │   │       └── TaskType.kt
        │       │   ├── engine/                    ← Task Engine interfaces
        │       │   │   ├── TaskEngine.kt
        │       │   │   └── TaskEvaluator.kt
        │       │   ├── repository/
        │       │   │   ├── ConfigRepository.kt
        │       │   │   ├── MonitoredAppsRepository.kt
        │       │   │   ├── SessionRepository.kt
        │       │   │   └── TaskRepository.kt      ← new
        │       │   └── usecase/
        │       │       ├── GetChildProgressUseCase.kt  ← new
        │       │       ├── GetInstalledAppsUseCase.kt
        │       │       ├── GetNextTaskUseCase.kt       ← new
        │       │       ├── ManageMonitoredAppsUseCase.kt
        │       │       └── SubmitTaskAnswerUseCase.kt  ← new
        │       │
        │       ├── data/
        │       │   ├── engine/                    ← Task Engine implementations
        │       │   │   ├── SimpleAdditionEvaluator.kt
        │       │   │   └── SimpleTaskEngine.kt
        │       │   ├── local/
        │       │   │   ├── db/
        │       │   │   │   ├── AppDatabase.kt
        │       │   │   │   ├── dao/
        │       │   │   │   │   ├── BlockSessionDao.kt
        │       │   │   │   │   ├── MonitoredAppDao.kt
        │       │   │   │   │   └── TamperEventDao.kt
        │       │   │   │   └── entity/
        │       │   │   │       ├── BlockSessionEntity.kt
        │       │   │   │       ├── MonitoredAppEntity.kt
        │       │   │   │       └── TamperEventEntity.kt
        │       │   │   └── prefs/
        │       │   │       └── SecurePreferencesManager.kt
        │       │   └── repository/
        │       │       ├── ConfigRepositoryImpl.kt
        │       │       ├── InMemoryTaskRepository.kt  ← new (MVP: static catalog)
        │       │       ├── MonitoredAppsRepositoryImpl.kt
        │       │       └── SessionRepositoryImpl.kt
        │       │
        │       ├── service/
        │       │   ├── AppUsageDetector.kt
        │       │   ├── KinderGateAccessibilityService.kt
        │       │   ├── MonitorService.kt
        │       │   └── SessionTimer.kt
        │       │
        │       ├── receiver/
        │       │   ├── BootReceiver.kt
        │       │   └── PackageEventReceiver.kt
        │       │
        │       ├── ui/
        │       │   ├── navigation/
        │       │   │   └── AppNavigation.kt
        │       │   └── theme/
        │       │       ├── Color.kt
        │       │       ├── Theme.kt
        │       │       └── Type.kt
        │       │
        │       ├── feature/
        │       │   ├── apppicker/
        │       │   │   ├── AppPickerScreen.kt
        │       │   │   └── AppPickerViewModel.kt
        │       │   ├── blocking/
        │       │   │   ├── BlockingActivity.kt     ← unchanged
        │       │   │   ├── BlockingScreen.kt       ← extended with TaskScreen widget
        │       │   │   └── BlockingViewModel.kt    ← drives task engine; zawiera sealed class BlockingEvent
        │       │   ├── dashboard/
        │       │   │   ├── DashboardScreen.kt
        │       │   │   └── DashboardViewModel.kt
        │       │   ├── onboarding/
        │       │   │   ├── OnboardingScreen.kt
        │       │   │   └── OnboardingViewModel.kt
        │       │   └── tasks/                     ← reusable task UI components
        │       │       ├── TaskScreen.kt           ← dispatches to per-type composables
        │       │       └── TaskUiState.kt          ← sealed UI state
        │       │
        │       └── di/
        │           ├── DatabaseModule.kt
        │           ├── RepositoryModule.kt
        │           └── TaskModule.kt              ← new; binds Task Engine + evaluator set
        │
        ├── test/java/pl/kindergate/
        │   ├── task/
        │   │   ├── SimpleAdditionEvaluatorTest.kt ← new
        │   │   └── SimpleTaskEngineTest.kt        ← new
        │   ├── SessionTimerTest.kt
        │   ├── ConfigRepositoryTest.kt
        │   └── MonitoredAppsRepositoryTest.kt
        │
        └── androidTest/java/pl/kindergate/
            ├── AppDatabaseTest.kt
            └── HiltTestRunner.kt
```

---

## 13. Lista decyzji architektonicznych (ADR)

### ADR-001: Hybrydowa detekcja foreground app (Accessibility primary + UsageStats fallback)

**Decyzja:** `AppUsageDetector` używa trzystopniowego priorytetu:
1. `KinderGateAccessibilityService.lastForegroundPackage` – natychmiastowy, zero-latency
2. `UsageStatsManager.queryEvents(now-10s, now)` – polling, ~1s opóźnienie
3. `UsageStatsManager.queryUsageStats(INTERVAL_DAILY)` – agregowane, ostateczny fallback

**Alternatywy:**
- UsageStats as primary (poprzednie założenie architektoniczne)
- `ActivityManager.getRunningTasks()` (deprecated API 21, unreliable)
- `ActivityManager.getRunningAppProcesses()` (nie daje foreground info)

**Uzasadnienie:**
- AccessibilityService daje natychmiastową detekcję (event-driven, TYPE_WINDOW_STATE_CHANGED) eliminując 1s opóźnienie
- UsageStats pozostaje jako fallback gdy Accessibility jest wyłączona – degradacja funkcjonalna zamiast awarii
- Połączenie obu metod daje maksymalne pokrycie OEM-specific restrictions (MIUI blokuje UsageStats; niektóre ROM-y utrudniają Accessibility)
- MonitorService dodatkowo subskrybuje `packageEvents` SharedFlow z AccessibilityService dla event-driven ticków niezależnie od pollingu

**Konsekwencje:**
- Wymaga zarówno `PACKAGE_USAGE_STATS` jak i (opcjonalnie) Accessibility Service
- Polling co 1s = ~10ms CPU overhead per second (akceptowalne dla foreground service)
- AccessibilityService jako primary → konieczność jasnego wyjaśnienia w Play Store description: "nie czyta treści aplikacji"

---

### ADR-002: Activity-based blocking (nie WindowManager overlay)

**Decyzja:** BlockingActivity zamiast `TYPE_APPLICATION_OVERLAY` przez WindowManager.

**Alternatywy:**
- `WindowManager` z `TYPE_APPLICATION_OVERLAY` (wymaga `SYSTEM_ALERT_WINDOW`)
- `KeyguardManager` (nie nadaje się do UI blokady)
- `DevicePolicyManager.lockNow()` (wymaga Device Admin)

**Uzasadnienie:**
- Activity jest bardziej stabilna (system zarządza cyklem życia)
- Nie wymaga `SYSTEM_ALERT_WINDOW` jako głównego mechanizmu
- Play Store jest sceptyczny wobec apps z `SYSTEM_ALERT_WINDOW`
- Activity z `FLAG_SHOW_WHEN_LOCKED` działa ponad lock screen
- Osobny `taskAffinity` zapobiega mieszaniu stacków

**Konsekwencje:**
- Dziecko może nacisnąć Home i ominąć blokadę na krótko
- MonitorService wykrywa powrót do monitored app i relaunches BlockingActivity
- Nie można zapobiec temu w 100% bez lock task mode

---

### ADR-003: SystemClock.elapsedRealtime() dla timera

**Decyzja:** Timer używa wyłącznie `SystemClock.elapsedRealtime()`.

**Alternatywy:**
- `System.currentTimeMillis()` – podatny na manipulację przez Settings
- `SystemClock.uptimeMillis()` – nie tickuje podczas sleep

**Uzasadnienie:**
- `elapsedRealtime()` to jedyna miara czasu immunna na zmiany przez użytkownika
- Startuje przy boocie, tickuje przez sleep, ignoruje NTP/timezone
- Dziecko nie może oszukać timera poprzez zmianę czasu w Settings

**Konsekwencje:**
- Timer resetuje się przy restarcie urządzenia (uptime resets)
- Nie ma sensu persistować stanu timera przez reboot
- Maks. strata: < 60s dla dziecka (akceptowalne w MVP)

---

### ADR-004: Hilt dla DI

**Decyzja:** Dagger Hilt zamiast Koin, manual DI, lub Dagger 2 raw.

**Alternatywy:**
- Koin: prostszy setup, ale słabszy compile-time verification
- Dagger 2 raw: pełna kontrola, ale boilerplate
- Manual DI: tylko dla trivially small apps

**Uzasadnienie:**
- Hilt = Dagger 2 z Android-specific integration (ViewModel, WorkManager, Service)
- Compile-time DI graph verification (błędy w build time, nie w runtime)
- `@HiltAndroidApp`, `@AndroidEntryPoint` dla services/receivers – clean integration
- Industry standard dla Android 2024+

**Konsekwencje:**
- KAPT/KSP compilation overhead (kompilacja wolniejsza o ~20%)
- Wymaga `@HiltAndroidApp` na Application class
- Każdy `Service`, `BroadcastReceiver` musi mieć `@AndroidEntryPoint`

---

### ADR-005: Brak Device Admin w MVP

**Decyzja:** MVP nie używa `DevicePolicyManager` / `DeviceAdminReceiver`.

**Alternatywy:**
- Device Admin: utrudnia odinstalowanie, wymaga PIN do dezaktywacji

**Uzasadnienie:**
- Google Play ogranicza Device Admin do enterprise/MDM apps
- Consumer parental control app z Device Admin → ryzyko odrzucenia lub usunięcia
- Wymaga dodatkowego onboarding (skomplikowany flow aktywacji)
- Dziecko z wiekiem i Google account może znaleźć jak to wyłączyć
- Filozofia produktu: transparentność > nieprzezroczysty lockdown

**Konsekwencje:**
- Odinstalowanie jest możliwe przez Settings > Apps
- Rekompensata: TamperEvent + powiadomienie rodzica

---

## 14. Ryzyka publikacji w Google Play

### Ryzyko wysokie

| Ryzyko | Mitigation |
|--------|-----------|
| `QUERY_ALL_PACKAGES` – wymaga deklaracji użycia | Justification w Play Console: "Needed to display installed apps to parent for monitoring configuration" |
| `PACKAGE_USAGE_STATS` – "protected permission" | Play Store pozwala na to dla parental control; należy to wyraźnie zaznaczyć w opisie aplikacji |
| AccessibilityService – Play Store może flagować | Opis: "Used only for fallback foreground detection, does not read content"; może wymagać manual review |
| Foreground service `specialUse` | Wymaga wypełnienia `PROPERTY_SPECIAL_USE_FGS_SUBTYPE` (zrobione w manifeście) |

### Ryzyko średnie

| Ryzyko | Mitigation |
|--------|-----------|
| `SYSTEM_ALERT_WINDOW` deklaracja | Wymagane tylko jako fallback; deklaracja jest OK jeśli jest powód |
| Wake lock | Akceptowalne dla monitoring app; foreground service = uzasadnienie |
| Targeting dzieci (8-15) | Aplikacja jest instalowana przez RODZICA na telefon dziecka; store listing nie targetuje dzieci bezpośrednio |

### Wymagania compliance

1. **Family policy**: Aplikacja powinna być w kategorii "Parental Control Tools", nie Family category
2. **Privacy policy**: Wymagana; dane lokalne, brak cloud sync = prosta policy
3. **Sensitive permissions declaration**: Przygotować opis każdego sensitive uprawnienia w Play Console

---

## 15. Następny krok po MVP → v1

### Priorytety v1 (kolejność)

1. **Warstwa edukacyjna (quizy)** ✅ MVP gotowe
   - Po każdych 60s: zamiast samego OK → zadanie dodawania (zaimplementowane, patrz sekcja 16)
   - Reward engine: dziecko "zarabia" dodatkowy czas za poprawne odpowiedzi (v1)
   - Architektura rozszerzalna: nowe typy zadań bez modyfikacji istniejącego kodu

2. **Panel rodzica w czasie rzeczywistym**
   - Websocket lub Firebase FCM do notyfikacji na telefon rodzica
   - Live dashboard z aktywnym session trackingiem

3. **Kumulatywne limity dzienne**
   - Zamiast per-session 60s: per-day 2h total
   - Wymaga persistencji `dailyUsageMs` (już częściowo w Room)

4. **Multi-device sync**
   - Konfiguracja backendowa (Firestore/Supabase)
   - Parent config sync między urządzeniami rodzica

5. **Device Admin (opcjonalne)**
   - Opt-in dla rodziców którzy chcą silniejszej ochrony
   - Clear UX explanation czym jest Device Admin

6. **OEM-specific workarounds**
   - Deep links do Xiaomi Security app, Huawei Phone Manager
   - Autodetect OEM i personalizuj onboarding

---

## Otwarte pytania produktowe

1. Co się dzieje kiedy dziecko używa aplikacji przez przeglądarkę (TikTok Lite web)?
2. Jak obsługujemy multiple profiles na jednym urządzeniu (Android managed profiles)?
3. Czy rodzic powinien widzieć, w jaki sposób dziecko próbowało ominąć ochronę?
4. Jaka jest polityka prywatności dla danych logów sesji?
5. Czy aplikacja ma działać na tabletach? (landscape mode dla BlockingActivity)

## Decyzje, które można podjąć bez blokowania implementacji

1. Dokładny target sek. dla bloku (60s / 45s / 90s) – `BuildConfig.BLOCK_INTERVAL_SECONDS`
2. Kolor schematu UI – zmiana w `Color.kt`
3. Jakie aplikacje są pre-oznaczone w app pickerze
4. Czy SHA-256 jest wystarczający dla MVP czy od razu Argon2
5. Nazwa użytkownika w Store: "KinderGate" czy "EduGate" czy inna

## Decyzje do podjęcia przed startem developmentu

1. **Docelowe urządzenia testowe** – minimum 3 (Pixel + Samsung + Xiaomi) do walidacji OEM issues
2. **Google Play Developer account** – czy mamy, czy potrzeba nowego (kategoria parental control)
3. **Backend plan** – czy MVP jest offline-only (YES, zgodnie z wymaganiami), czy jest Firebase od początku
4. **Privacy policy URL** – wymagane przez Play Store przed publikacją
5. **Podejście do MIUI autostart** – czy wymagamy tego od rodzica w onboardingu, czy jest to "bonus"

## Sekcje wymagające przepuszczenia przez Opus (głębsza analiza)

1. **Quiz/reward engine architektura** – Knowledge tracing, Leitner system, spaced repetition dla pytań edukacyjnych. Wymaga przemyślanego modelu domenowego i prawdopodobnie ML backend.
2. **OEM-specific background service survival** – Kompletna strategia dla MIUI 14+, EMUI 13+, ColorOS 14 z aktualnymi obejściami i automatyczną detekcją ograniczeń.
3. **Multi-account i Android Enterprise** – Czy i jak obsługiwać managed profiles, work profiles, secondary accounts.
4. **Backend i sync architektura** – Kiedy dodać backend, jaki stack, jak robić real-time notifications do rodzica, privacy-first approach.
5. **Monetyzacja i freemium model** – Gdzie ciąć funkcje, jak implementować paywall bez naruszania UX, compliance z Google Play billing.

---

## 16. Task Engine – warstwa zadań edukacyjnych

> Wersja: 0.1.0-mvp · Dodana: 2026-03

### Cel

Zamiast ekranu PAUZA z samym przyciskiem „OK, rozumiem" dziecko dostaje krótkie zadanie edukacyjne (MVP: dodawanie w pamięci). Poprawna odpowiedź odblokowuje ekran. Cała logika zadań jest izolowana od warstwy monitoringu – `MonitorService`, `SessionTimer` i `TamperDetector` pozostają niezmienione.

---

### Architektura poddomeny

```
┌──────────────────────────────────────────────────────────────┐
│                    FEATURE / BLOCKING                         │
│  BlockingActivity (unchanged)                                 │
│    └─ BlockingScreen ──→ BlockingViewModel                    │
│                               ├─ GetNextTaskUseCase           │
│                               └─ SubmitTaskAnswerUseCase      │
└──────────────────────────────────┬───────────────────────────┘
                                   │ domain contracts only
┌──────────────────────────────────▼───────────────────────────┐
│                    DOMAIN / ENGINE                            │
│  TaskEngine (interface)   TaskEvaluator (interface)           │
│  TaskRepository (interface)                                   │
│  Use cases: GetNextTask / SubmitAnswer / GetChildProgress      │
│  Models: Task, TaskContent, EvaluationResult, ChildProgress… │
└──────────────────────────────────┬───────────────────────────┘
                                   │ Hilt @Binds
┌──────────────────────────────────▼───────────────────────────┐
│                    DATA / ENGINE                              │
│  SimpleTaskEngine       – adaptive difficulty, in-memory log  │
│  SimpleAdditionEvaluator – stateless; handles SIMPLE_ADDITION │
│  InMemoryTaskRepository  – 45 curated tasks, levels 1–3       │
└──────────────────────────────────────────────────────────────┘
```

---

### Modele domenowe (domain/model/task/)

| Klasa | Rola |
|-------|------|
| `Task` | Główny model zadania; nie ma adnotacji Room |
| `TaskContent` | Sealed class – polimorficzny payload pytania (MVP: `SimpleAdditionContent`) |
| `TaskSource` | Enum – skąd pochodzi zadanie (MVP: `APP_LIBRARY`) |
| `TaskSubject` | Enum – przedmiot (MVP: `MATH`) |
| `CognitiveSkill` | Enum – umiejętność (MVP: `SIMPLE_ARITHMETIC`) |
| `DifficultyMode` | Enum – jak ustalany jest poziom (MVP: `MANUAL`) |
| `TaskType` | Enum – format zadania / który evaluator obsługuje (MVP: `SIMPLE_ADDITION`) |
| `TaskMetadata` | Opcjonalne metadane (gradeLevel, tags) – nie wpływają na logikę |
| `TaskContext` | Hint przekazywany do silnika (MVP: `preferredDifficultyLevel`) |
| `TaskSet` | Nazwana kolekcja zadań z zakresu trudności |
| `EvaluationResult` | Wynik ewaluacji jednej odpowiedzi |
| `ChildProgress` | Zagregowany postęp dziecka |

---

### Interfejsy domenowe

#### TaskRepository
```kotlin
interface TaskRepository {
    suspend fun getTaskById(id: String): Task?
    suspend fun getTasksForSet(taskSetId: String): List<Task>
    suspend fun getRandomTask(subject: TaskSubject, difficultyLevel: Int): Task?
}
```

#### TaskEngine
```kotlin
interface TaskEngine {
    suspend fun getNextTask(childId: String, context: TaskContext? = null): Task
    suspend fun submitAnswer(childId: String, taskId: String, answer: String): EvaluationResult
    suspend fun getChildProgress(childId: String): ChildProgress
}
```

#### TaskEvaluator
```kotlin
interface TaskEvaluator {
    fun canEvaluate(task: Task): Boolean
    fun evaluate(task: Task, answer: String): EvaluationResult
}
```

---

### Implementacje MVP

#### InMemoryTaskRepository
- 45 pre-seeded zadań (15 × 3 poziomy)
- Poziom 1: sumy ≤ 10 | Poziom 2: sumy ≤ 20 | Poziom 3: sumy ≤ 100
- Stabilne ID (`add_l1_0`…`add_l3_14`) → `getTaskById` działa niezawodnie
- Migracja do Room: zaimplementuj `RoomTaskRepository : TaskRepository`, zmień binding w `TaskModule`

#### SimpleAdditionEvaluator
- Bezstanowy, thread-safe
- Poprawność: `answer.trim().toIntOrNull() == correctAnswer`
- Feedback po polsku, niekarający

#### SimpleTaskEngine (adaptacyjna trudność)
```
getNextTask(childId, context):
  1. Pobierz aktywny poziom (lub z context.preferredDifficultyLevel, lub MIN=1)
  2. Sprawdź ostatnich WINDOW_SIZE=5 odpowiedzi:
     - wszystkie poprawne → poziom +1 (max 3)
     - > FAILURE_THRESHOLD=2 błędów → poziom -1 (min 1)
     - else → bez zmian
  3. Zwróć losowe zadanie z repozytorium dla subject=MATH + wybrany poziom

submitAnswer(childId, taskId, answer):
  1. Pobierz zadanie z repozytorium
  2. Znajdź pasujący evaluator (Set<TaskEvaluator> via Hilt multibinding)
  3. Evaluuj, zapisz do historii in-memory
  4. Przekalkuluj poziom
  5. Zwróć EvaluationResult
```

Historia przechowywana in-memory (`ConcurrentHashMap`) – reset po process death. W v1: persystuj do Room.

---

### DI – TaskModule

```kotlin
@Module @InstallIn(SingletonComponent::class)
abstract class TaskModule {
    @Binds @Singleton abstract fun bindTaskRepository(impl: InMemoryTaskRepository): TaskRepository
    @Binds @Singleton abstract fun bindTaskEngine(impl: SimpleTaskEngine): TaskEngine
    @Binds @IntoSet    abstract fun bindSimpleAdditionEvaluator(impl: SimpleAdditionEvaluator): TaskEvaluator
}
```

Dodanie nowego typu zadania = nowy `@Binds @IntoSet` wpis. Żadne istniejące klasy nie wymagają zmiany.

---

### Integracja z BlockingActivity

```
BlockingActivity (unchanged)
  └─ setContent { BlockingScreen(onAcknowledge = ::onChildAcknowledged) }
       └─ hiltViewModel<BlockingViewModel>()
            ├─ uiState: StateFlow<TaskUiState>
            │    ├─ Loading   → CircularProgressIndicator
            │    ├─ ShowingTask → TaskScreen (feature/tasks)
            │    └─ Error     → fallback OK button (dziecko nie utknęło)
            └─ events: SharedFlow<BlockingEvent>
                 └─ TaskSolvedCorrectly → onAcknowledge() → MonitorService.ACTION_BLOCK_ACKNOWLEDGED
```

`BlockingActivity.onChildAcknowledged()` i `MonitorService` są bez zmian.

---

### ADR-006: TaskEvaluator jako Set via Hilt multibinding

**Problem**: jak dodawać nowe typy zadań bez modyfikacji `SimpleTaskEngine`?

**Decyzja**: Każdy `TaskEvaluator` jest wstrzykiwany jako element `Set<TaskEvaluator>` przez Hilt `@IntoSet`. Silnik iteruje zbiór i wywołuje `canEvaluate()` żeby wybrać właściwy.

**Konsekwencje**:
- Dodanie nowego zadania = nowa klasa + jeden wpis w `TaskModule` — open/closed principle
- Minimalny narzut: zbiór jest mały (kilka–kilkanaście evaluatorów)
- Alternatywa odrzucona: mapa `TaskType → TaskEvaluator` w Module – silnie sprzężona z enum, trudna do testowania

---

### Plan rozbudowy (v1 → v2)

| Faza | Co dodać | Gdzie |
|------|----------|-------|
| v1 | Persystencja historii w Room | Nowe `TaskHistoryEntity` + `RoomTaskEngine` |
| v1 | Odejmowanie, mnożenie | Nowe `TaskType`, `TaskContent` subtype, nowy `TaskEvaluator` |
| v1 | Reward: +30s za poprawną odpowiedź | `BlockingViewModel` + `SessionTimer.addBonus(ms)` |
| v2 | AI-generowane zadania | `AiTaskRepository : TaskRepository` |
| v2 | Wieloprzedmiotowe zadania | `TaskSubject.READING` itd. + odpowiednie evaluatory |
| v2 | Wybór poziomu przez rodzica | `TaskContext.preferredDifficultyLevel` z Dashboard |
| v3 | Spaced repetition | Wymień `SimpleTaskEngine` na `SpacedRepetitionTaskEngine` |
