# mokoMusic

Android Jetpack Compose와 Media3로 구현한 로컬 음악 플레이어입니다. ExoPlayer를 `MediaSessionService`에서 구동해 백그라운드 재생과 Android 시스템 미디어 제어를 연결합니다.

## 주요 기능

- 재생 대기열, 탐색, 이전·다음 곡, 셔플·반복
- 시스템 미디어 알림과 MediaSession 제어
- 재생 곡·위치·셔플·반복 상태 복원
- MediaController 연결 실패 안내와 재연결
- 즐겨찾기와 최근 재생 기록 저장
- 재생 오류 안내와 다시 시도
- APK에 포함한 CC0 음원 3곡의 오프라인 재생

## 기술 스택과 선택 이유

| 기술 | 적용 위치 | 선택 이유 |
| --- | --- | --- |
| Kotlin | 앱 전체 | Android와 Media3 API를 직접 사용하면서 nullable 타입과 코루틴 등 Kotlin 언어 기능으로 상태·비동기 처리를 표현합니다. |
| Jetpack Compose, Material 3 | `MainActivity` 플레이어 화면 | 재생 상태를 화면에 선언적으로 반영하고, 버튼·슬라이더·곡 목록을 작은 UI 요소로 구성합니다. |
| AndroidX Lifecycle `ViewModel`, Kotlin Coroutines `StateFlow` | `PlaybackViewModel` | 화면 상태를 Activity보다 오래 유지되는 ViewModel에 모으고, Compose가 생명주기에 맞춰 상태를 관찰하도록 합니다. |
| AndroidX Media3 ExoPlayer | `PlaybackService` | 곡 재생, 탐색, 대기열, 셔플·반복을 처리하는 실제 플레이어입니다. Media3의 기본 데이터 소스로 APK assets의 음원을 읽습니다. |
| AndroidX Media3 `MediaSessionService`, `MediaSession`, `MediaController` | 서비스와 화면 사이 | 플레이어를 화면 생명주기와 분리해 백그라운드에서 유지하고, 시스템 미디어 제어와 앱 UI가 같은 플레이어를 조작하도록 연결합니다. |
| SharedPreferences | `PlaybackStateStore`, `SharedPreferencesLibraryRepository` | 재생 위치·모드와 즐겨찾기·최근 곡 ID처럼 규모가 작은 로컬 상태를 별도 서버나 데이터베이스 없이 저장합니다. |
| JUnit 4 | Playback 관련 단위 테스트 | Media3 오류 코드 안내 문구와 컨트롤러 연결 상태 전이를 빠르게 검증합니다. |

현재 곡 정보는 APK에 포함된 로컬 음원 세 곡으로 한정됩니다. 별도 백엔드나 스트리밍 API를 두지 않아 시스템 재생 연동에 집중할 수 있고, 샘플 재생에는 네트워크가 필요하지 않습니다. 곡 수가 커지거나 검색·다운로드·동기화가 필요해지면 카탈로그와 저장 계층을 확장해야 합니다.

## 구조

```text
┌──────────────────────────── 화면 ────────────────────────────┐
│ MainActivity / PlaybackScreen                                │
│ Compose UI · PlaybackUiState 관찰 · 사용자 입력 전달          │
└─────────────────────────────┬────────────────────────────────┘
                              │ 명령
                              ▼
┌──────────────────────── 상태·제어 ──────────────────────────┐
│ PlaybackViewModel ── MediaController                         │
│ StateFlow 상태 구성       MediaSession에 재생 명령 전달       │
└─────────────────────────────┬────────────────────────────────┘
                              │ MediaSession 연결
                              ▼
┌──────────────────────── 재생 서비스 ────────────────────────┐
│ PlaybackService ── MediaSession ── ExoPlayer                 │
│ 백그라운드 수명 관리     시스템 제어 창구    재생·대기열 처리  │
└─────────────────────────────┬────────────────────────────────┘
                              │ 데이터 소스
                              ▼
                    APK assets의 MP3 파일

SampleTrackRepository ── 곡 ID·제목·아티스트·asset URI
PlaybackStateStore ───── 재생 곡·위치·셔플·반복 설정
LibraryRepository ────── 즐겨찾기·최근 재생 곡 ID
```

### 컴포넌트 책임

- `MainActivity`: `PlaybackUiState`를 수집해 Compose 화면을 그리고, 입력을 ViewModel 함수에 연결합니다.
- `PlaybackViewModel`: `MediaController`로 재생 명령을 전달하고 플레이어 이벤트를 `StateFlow<PlaybackUiState>`로 변환합니다. 연결 Future의 성공·실패를 상태에 반영하고 실패 시 재시도를 시작합니다. 화면이 플레이어 구현을 직접 소유하지 않습니다.
- `ControllerConnectionUiState`: 연결 중·연결됨·실패 상태와 오류 문구를 표현합니다. 재시도가 시작되거나 연결에 성공하면 이전 오류 상태를 지웁니다.
- `PlaybackService`: `ExoPlayer`와 `MediaSession`의 수명 주기를 관리합니다. 플레이어가 UI가 보이지 않을 때도 재생할 수 있게 하고 시스템과 외부 컨트롤러에 세션을 제공합니다.
- `SampleTrackRepository`, `Track`: 재생 목록의 곡 메타데이터와 `asset:///audio/...` URI를 정의하고 Media3 `MediaItem`으로 변환합니다.
- `PlaybackStateStore`: 현재 곡 ID, 재생 위치, 셔플·반복을 저장하고 서비스 시작 시 복원합니다. 복원 후 자동 재생하지 않고 준비된 일시정지 상태를 유지합니다.
- `LibraryRepository`: 즐겨찾기와 최근 재생 ID를 로컬에 저장합니다. 현재 구현은 SharedPreferences를 사용합니다.
- `PlaybackErrorMessage`: Media3 오류 코드를 안내 문구로 바꾸며, 매핑 규칙은 단위 테스트에서 확인합니다.

### 재생 및 상태 흐름

1. 사용자가 화면에서 곡을 선택하거나 재생 버튼을 누릅니다.
2. `PlaybackScreen`이 이벤트를 `PlaybackViewModel`에 전달합니다.
3. ViewModel은 `MediaController`를 통해 `PlaybackService`의 `MediaSession`에 명령을 보냅니다.
4. 서비스가 소유한 ExoPlayer가 대기열을 조작하고 APK assets에서 음원을 재생합니다.
5. 플레이어 이벤트를 ViewModel이 `PlaybackUiState`로 변환합니다. Compose 화면은 생명주기에 맞춰 상태를 수집해 제목, 진행 위치, 버튼 상태를 갱신합니다.
6. MediaSession은 같은 재생 상태와 제어 명령을 Android 미디어 알림 및 호환되는 시스템 컨트롤에 제공합니다.

플레이어 이벤트와 주기 저장 작업이 재생 위치·곡 정보를 저장합니다. 즐겨찾기와 최근 재생은 별도 Repository를 통해 관리해 재생 상태와 라이브러리 상태의 책임을 나눴습니다.

## 빌드와 테스트

- Android Gradle Plugin: 9.2.1
- Kotlin: 2.2.10
- Android 최소 API: 26
- Compile SDK: 37 / Target SDK: 36

Android Studio에서 프로젝트를 열거나, Windows PowerShell에서 저장소 루트에서 실행합니다.

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat testDebugUnitTest
```

디버그 APK: `app/build/outputs/apk/debug/app-debug.apk`

## 검증 범위

- 빌드와 단위 테스트 통과
- 에뮬레이터에서 재생 상태·위치 진행, MediaSession 다음 곡 전환, 백그라운드·화면 꺼짐 재생, 재생 상태 복원 확인
- 즐겨찾기 저장·복원과 오류 문구 분류 확인
- 컨트롤러 연결 상태 전이 단위 테스트
- MediaController 정상 연결과 MediaSession 등록을 에뮬레이터에서 확인
- 연결 Future 실패를 실제로 재현해 UI에서 다시 연결하는 흐름은 검증하지 못했습니다.

## 샘플 음원과 라이선스

Suika, Black Hole, Galaxy Collision은 hyak의 CC0 1.0 BGM Pack v1에서 가져온 음원입니다. 원본 파일명, 크기, 출처와 라이선스는 [`docs/audio-samples.md`](docs/audio-samples.md)에 기록했습니다. 오디오 내용은 편집하지 않았습니다.
