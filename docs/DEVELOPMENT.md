# Development Guide

## 개발 환경
- JDK: 21
- Gradle wrapper: 저장소 내 `./gradlew` 사용
- Android Studio 최신 안정 버전 권장

## 자주 쓰는 명령
- 플러그인 로컬 게시:
  - `./gradlew :wspb-gradle-plugin:publishToMavenLocal --configure-on-demand`
- 샘플 앱 빌드:
  - `./gradlew :sample-app:assembleDebug`
- 전체 포맷 검사:
  - `./gradlew spotlessCheck`
- 포맷 자동 적용:
  - `./gradlew spotlessApply`
- Android lint:
  - `./gradlew lint`

## 권장 작업 순서
1. 변경 대상 모듈 선택 (`annotation` / `processor` / `plugin`)
2. 샘플 앱 케이스로 재현/검증
3. 포맷 검사와 lint 통과 확인
4. 문서(`README`, `docs/*`) 동기화

## 모듈별 수정 포인트
- 애노테이션 변경:
  - `wspb-annotation/src/main/...`
  - 사용 예시를 `sample-app`에 반영
- 프로세서 변경:
  - `wspb-processor/src/main/...`
  - 산출 proto/생성 코드 경로를 실제 빌드로 확인
- 플러그인 변경:
  - `wspb-gradle-plugin/src/main/...`
  - variant별 sourceSet 연결 여부 검증

## 테스트 현황
현재 기본 테스트 소스는 많지 않으므로, 기능 수정 시 최소한 아래 검증을 수행합니다.
- `:sample-app:assembleDebug`
- 생성 산출물 위치/내용 점검
- `spotlessCheck`, `lint`

## CI 참고
`.github/workflows/Build.yml` 기준:
1. plugin publish to mavenLocal
2. spotlessCheck
3. lint
4. sample-app assemble
