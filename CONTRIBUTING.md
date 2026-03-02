# Contributing

## 기본 원칙
- 변경은 가능한 한 작고 명확하게 유지합니다.
- 코드 변경 시 관련 문서도 함께 업데이트합니다.
- `sample-app`은 라이브러리 사용 예시이므로, 본체 로직과 책임을 분리해 작업합니다.

## 개발 플로우
1. 브랜치 생성
2. 코드 수정
3. 로컬 검증
4. PR 생성

## 로컬 검증 체크리스트
```bash
./gradlew :wspb-gradle-plugin:publishToMavenLocal --configure-on-demand
./gradlew :sample-app:assembleDebug
./gradlew spotlessCheck
./gradlew lint
```

## 커밋 가이드
- 작은 단위로 커밋
- 메시지는 변경 의도가 드러나게 작성
  - 예: `Add proto mapping for ByteArray`

## Pull Request 가이드
- PR 템플릿 항목을 빠짐없이 채웁니다.
- 변경 범위와 검증 결과를 명확히 남깁니다.
- Breaking change가 있다면 영향 범위와 마이그레이션 방법을 반드시 작성합니다.

## 이슈 리포트 가이드
- 재현 가능한 최소 단계 제공
- 환경 정보(JDK/Gradle/AGP/Kotlin) 포함
- 관련 로그 또는 스택트레이스 첨부
