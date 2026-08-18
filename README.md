# SMS 전달 앱 (SmsRelay)

특정 발신번호로 오는 SMS를 **폰A → 폰B**로 전달하는 안드로이드 앱. APK 하나로 두 역할을 모두 지원한다.

- **폰A (수신기)**: 문자를 받아, 화이트리스트에 등록한 발신번호와 일치하면 전달
- **폰B (표시기)**: 전달된 문자를 **원 발신번호를 제목으로** 알림·목록에 표시

전송은 Firebase Realtime Database 실시간 리스너로 중계한다. **별도 서버가 필요 없다.**

---

## ⚠️ 먼저 알아둘 것 (중요)

- **발신번호를 원래 문자 발신번호로 "재발송"하는 것은 불가능하다.** 안드로이드 `SmsManager`는 항상 기기 SIM 번호로만 발신하고, 발신번호 지정 API가 없다. 또 한국 전기통신사업법상 발신번호 거짓표시는 금지다.
- 그래서 이 앱은 **문자를 다시 쏘지 않고**, 폰B에서 **원 발신번호가 제목인 알림**으로 띄운다. 받는 쪽 화면에서는 원래 번호가 그대로 보인다. 문자 요금도 안 든다.
- OTP·인증번호가 화이트리스트에 걸리면 보안 위험이 있으니 전달 번호는 좁게 잡을 것.

---

## 1. Firebase 설정 (5분, 무료)

1. https://console.firebase.google.com 접속 → **프로젝트 만들기**
2. 좌측 메뉴 **빌드 → Realtime Database → 데이터베이스 만들기**
   - 위치는 아무거나(예: 싱가포르), **"잠금 모드"**로 시작
3. **규칙(Rules)** 탭에서 아래로 교체 후 게시:

   ```json
   {
     "rules": {
       "relay": {
         "$pair": {
           ".read": true,
           ".write": true,
           "messages": {
             ".indexOn": ["ts"]
           }
         }
       }
     }
   }
   ```

   > 개인용 2대라면 이 정도로 충분하다. 페어 코드를 남이 모르면 접근 못 한다.
   > 더 조이려면 Firebase Auth(익명 로그인)로 `.read/.write`를 `auth != null`로 바꾸면 된다.

4. 프로젝트 개요 옆 톱니 → **프로젝트 설정 → 내 앱 → Android 앱 추가**
   - **Android 패키지 이름**에 반드시 `com.tossinsu.smsrelay` 입력
   - `google-services.json` 다운로드 → 이 프로젝트의 **`app/` 폴더**에 넣기
   - (동봉된 `app/google-services.json.SAMPLE`은 예시일 뿐이니 지우거나 무시)

---

## 2. 빌드 (Android Studio)

1. Android Studio(최신) 설치
2. **File → Open** → 이 `SmsRelay` 폴더 선택 → Gradle Sync 완료까지 대기
   (Gradle 래퍼는 첫 실행 시 자동 생성된다)
3. `app/google-services.json`이 들어있는지 확인
4. 상단 실행(▶) 또는 **Build → Build APK(s)**
   - 산출물: `app/build/outputs/apk/debug/app-debug.apk`

명령줄로 빌드하려면 프로젝트 폴더에서:

```bash
./gradlew assembleDebug      # macOS/Linux
gradlew.bat assembleDebug    # Windows
```

---

## 3. 설치 & 설정

두 폰 모두 같은 APK를 설치한다. (설정 → 보안 → **출처를 알 수 없는 앱 설치 허용**)

### 폰A (수신기)
1. 앱 실행 → 역할 **① 수신기** 선택
2. **페어 코드** 입력 (예: `mypair-8271`) — 폰B와 반드시 동일하게
3. **전달할 발신번호** 입력 (쉼표/줄바꿈으로 여러 개, 하이픈 무관)
4. (선택) "매칭 테스트"로 특정 번호가 걸리는지 확인
5. **저장 & 시작** → SMS/알림 권한 허용 → 배터리 최적화 예외 허용

### 폰B (표시기)
1. 앱 실행 → 역할 **② 표시기** 선택
2. **페어 코드**를 폰A와 **똑같이** 입력
3. **저장 & 시작** → 알림 권한 허용 → 배터리 최적화 예외 허용

이제 폰A에 등록 번호로 문자가 오면, 폰B에 원 발신번호 제목으로 알림이 뜨고 앱 목록에도 쌓인다.

---

## 4. 안정적으로 계속 돌리기 (필수 팁)

백그라운드에서 죽지 않게 두 폰 모두:

- **배터리 최적화 예외**: 설정 → 배터리 → 앱별 → SMS 전달 → "제한 없음"
- **자동 시작 허용**(삼성/샤오미/오포 등 국내외 제조사 UI에 따라 위치 다름)
- 폰B는 가급적 충전 상태로 켜두기 (Wi-Fi/데이터 연결 필요)
- 재부팅 후에는 자동으로 서비스가 다시 뜨지만, 제조사에 따라 한 번 앱을 열어줘야 할 수 있다

---

## 5. 파일 구조

```
SmsRelay/
├─ app/
│  ├─ google-services.json      ← 본인이 넣어야 함 (Firebase에서 다운로드)
│  ├─ google-services.json.SAMPLE
│  ├─ build.gradle.kts
│  └─ src/main/
│     ├─ AndroidManifest.xml
│     ├─ java/com/tossinsu/smsrelay/
│     │  ├─ MainActivity.kt        설정 화면 · 권한 · 목록
│     │  ├─ Prefs.kt               역할/페어코드/화이트리스트 저장, 번호 매칭
│     │  ├─ SmsReceiver.kt         문자 수신 → 화이트리스트 필터 → RTDB 전송
│     │  ├─ SenderService.kt       폰A 상시 실행 서비스
│     │  ├─ ReceiverService.kt     폰B RTDB 리스너 → 알림·저장
│     │  ├─ NotificationHelper.kt  알림 채널/생성
│     │  ├─ MessageStore.kt        받은 문자 로컬 보관
│     │  ├─ MessageAdapter.kt      목록 표시
│     │  ├─ FirebaseRefs.kt        RTDB 경로
│     │  ├─ RelayMessage.kt        데이터 모델
│     │  └─ BootReceiver.kt        재부팅 시 자동 재시작
│     └─ res/                      레이아웃·아이콘·문자열
├─ build.gradle.kts
├─ settings.gradle.kts
└─ gradle/wrapper/gradle-wrapper.properties
```

---

## 6. 문제 해결

| 증상 | 확인 |
|---|---|
| 폰B에 안 옴 | 두 폰 페어 코드 동일? / 폰A가 "수신기", 폰B가 "표시기"? / 둘 다 인터넷 연결? |
| 문자는 오는데 전달 안 됨 | 발신번호가 화이트리스트에 있나? "매칭 테스트"로 확인 |
| 조금 있다 멈춤 | 배터리 최적화 예외·자동 시작 허용했는지 |
| 빌드 실패 | `app/google-services.json` 있는지 / 패키지명 `com.tossinsu.smsrelay` 맞는지 |

---

## 7. 한계

- **iOS는 불가능** (앱이 SMS를 읽는 API 자체가 없음). 이 앱은 안드로이드 전용.
- Google Play 정책상 SMS 권한 앱은 기본 문자 앱이 아니면 심사가 어렵다 → **APK 직접 설치 전용**.
- 발신번호 표시는 "알림 제목"으로만 재현되며, 실제 문자 스레드로는 들어가지 않는다.
