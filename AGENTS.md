# Repository Guidelines

## 목적
이 문서는 저장소에서 작업하는 사람/에이전트가 빠르게 일관된 방식으로 기여할 수 있도록 운영 규칙을 정리합니다.

## 저장소 성격
- 본체: `wspb-annotation`, `wspb-processor`, `wspb-gradle-plugin`
- 예시: `sample-app` (라이브러리 소비자 사용 예시 앱)

## 작업 원칙
- 라이브러리 동작 변경은 본체 모듈 위주로 수정하고, `sample-app`은 검증 용도로 유지합니다.
- 문서와 코드가 어긋나지 않게 함께 업데이트합니다.
- 동작 확인은 가능한 최소 명령으로 빠르게 검증합니다.

## 표준 검증 명령어
```bash
./gradlew :wspb-gradle-plugin:publishToMavenLocal --configure-on-demand
./gradlew :sample-app:assembleDebug
./gradlew spotlessCheck
./gradlew lint
```

## 자주 발생하는 이슈
- `Internal schema project ... was not found`:
  - 플러그인 해석 불일치 가능성이 큽니다.
  - 먼저 `:wspb-gradle-plugin:publishToMavenLocal --configure-on-demand` 실행 후 재시도합니다.

## 문서 위치
- 개요/빠른 시작: `README.md`
- 설계: `docs/ARCHITECTURE.md`
- 사용법: `docs/USAGE.md`
- 개발/기여: `docs/DEVELOPMENT.md`
- 트러블슈팅: `docs/TROUBLESHOOTING.md`
