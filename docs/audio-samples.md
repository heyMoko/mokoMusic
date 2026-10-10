# 오디오 샘플 기록

앱에는 `hyak`가 제작한 다음 CC0 1.0 원곡 세 곡을 MP3로 포함합니다. 제작자는 여섯 곡 모두 완전히 독창적인 곡이며, 외부 음원이나 제3자 자료를 사용하지 않았다고 밝히고 CC0 1.0으로 공개했습니다. 재생·수정·배포에 별도 허가나 출처 표시는 필요하지 않지만, 출처 확인을 돕기 위해 이 문서에 기록합니다.

| 앱 파일 | 앱 표시 제목 | 원본 파일명 | 길이 | 크기 |
| --- | --- | --- | --- | ---: |
| `app/src/main/assets/audio/suika.mp3` | Suika | `hyak-ep3-suika.mp3` | 2:40 | 6,402,132 bytes |
| `app/src/main/assets/audio/blackhole.mp3` | Black Hole | `hyak-ep4-blackhole.mp3` | 2:38 | 6,338,394 bytes |
| `app/src/main/assets/audio/galaxy.mp3` | Galaxy Collision | `hyak-ep6-galaxy.mp3` | 2:40 | 6,402,132 bytes |

- 제작자: hyak
- 원본·라이선스 설명: [hyak CC0 BGM Pack v1](https://github.com/uncle-sheepsky/hyak-cc0-bgm/releases/tag/v1)
- 배포 MP3 팩: [hyak-cc0-bgm-mp3.zip](https://github.com/uncle-sheepsky/hyak-cc0-bgm/releases/download/v1/hyak-cc0-bgm-mp3.zip)
- 라이선스: [CC0 1.0 Universal](https://creativecommons.org/publicdomain/zero/1.0/)
- 변경 사항: 배포 ZIP에서 위 3개 MP3를 추출해 파일명만 앱용으로 바꿨습니다. 오디오 내용은 편집하지 않았습니다.

재생은 `SampleTrackRepository`가 `asset:///audio/...` URI와 실제 곡 메타데이터를 `Track`에 연결하고, `PlaybackService`의 Media3 기본 데이터 소스가 APK의 assets에서 파일을 읽는 방식입니다. 앱 실행 중 네트워크 연결은 필요하지 않습니다.
