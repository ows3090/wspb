# Repository Guidelines

## 프로젝트 개요
이 저장소는 오픈소스 안드로이드용 라이브러리 WSPB(Working with KSP + Protobuf)와 샘플 앱으로 구성됩니다.
- 목표: Kotlin 데이터 클래스를 `@WSProto`로 표기하면, KSP가 `.proto` 파일을 생성하고 Gradle 플러그인이 이를 Protobuf 컴파일 파이프라인에 연결해 주는 자동화 흐름 제공.
- `sample-app/`은 라이브러리와 KSP 프로세서를 실제로 적용해 동작을 검증하는 테스트 앱입니다.

## 동작 원리 (사용자에게 설명할 핵심 포인트)
- `wspb-annotation/`에 정의된 `@WSProto(name = ...)`를 데이터 클래스에 붙입니다.
- `wspb-processor/`의 KSP 프로세서가 어노테이션을 탐지해 `.proto` 파일을 생성합니다.
  - 생성 위치: `build/generated/ksp/<variant>/resources/proto/` 아래.
  - 패키지: `proto/com/wonseok/wspb`
  - 파일명: 어노테이션의 `name` 값.
  - 메시지 이름: `name` 값을 snake_case에서 PascalCase로 변환한 값.
  - 규칙: 파일명에서 변환된 메시지 이름과 클래스 이름이 같으면 오류로 중단합니다.
- `wspb-gradle-plugin/`이 Protobuf 플러그인을 적용하고, 생성된 `.proto` 경로를 Android `sourceSets`의 `proto` 소스로 등록합니다.
  - `protoc`는 `protobuf-protoc` 의존성을 사용.
  - `java` builtin을 `lite` 옵션으로 설정.
  - `protobuf-kotlin-lite` 런타임을 의존성에 추가.

## 지원 타입
- 기본 타입: `Int/Short/Byte -> int32`, `Long -> int64`, `Float`, `Double`, `Boolean`, `String`, `ByteArray -> bytes`
- 컬렉션: `List`, `Set`, `Array`는 `repeated`로 변환되며, 내부 타입도 동일 매핑을 따릅니다.
- 그 외 타입은 현재 지원하지 않으며 오류 처리됩니다.

## 프로젝트 구조 & 모듈 구성
멀티 모듈 Gradle/Kotlin 레포지토리입니다.
- `sample-app/`: 라이브러리 테스트용 Android 앱
- `wspb-annotation/`: 소비자가 사용하는 어노테이션 모듈
- `wspb-processor/`: KSP 프로세서 (Proto 파일 생성)
- `wspb-gradle-plugin/`: Gradle 플러그인 (Protobuf 연동)
- `gradle/` 및 `gradle/libs.versions.toml`: Gradle wrapper 및 버전 카탈로그
- `.github/`: CI 워크플로우 및 PR 템플릿

## 빌드/테스트/개발 명령어
레포 루트에서 Gradle wrapper 사용.
- `./gradlew :wspb-gradle-plugin:publishToMavenLocal` — 플러그인 Maven Local 배포 (CI 및 앱 빌드 전 유용)
- `./gradlew spotlessCheck` — 포맷 검사
- `./gradlew spotlessApply` — Kotlin 및 Gradle Kotlin DSL 자동 포맷
- `./gradlew lint` — Android Lint 실행
- `./gradlew :sample-app:assemble` — 샘플 앱 빌드

## 코딩 스타일 & 네이밍
- Kotlin/Gradle Kotlin DSL은 Spotless + ktlint 기준 준수 (`build.gradle.kts`).
- Java/Kotlin 타겟 버전은 전 모듈 공통으로 21.
- Kotlin 네이밍 규칙(타입 PascalCase, 멤버 camelCase) 준수.

## 테스트 가이드
- 의존성에는 JUnit, AndroidX test/espresso가 포함되어 있으나 현재 테스트 소스는 없습니다.
- 테스트 추가 시 표준 Gradle 태스크 사용:
  - JVM 테스트: `./gradlew test`
  - 계측 테스트: `./gradlew connectedAndroidTest`
- 테스트 클래스는 `*Test.kt` 네이밍을 사용.

## 커밋 & PR 가이드
- 커밋 메시지는 짧고 명령형: `Add ...`, `Update ...`, `Apply ...` 형태 권장.
- 이슈/PR은 `#<id>` 형식으로 참조.
- PR 템플릿: `.github/pull_request_template.md ` (파일명 끝 공백 주의)
  - 작업 요약
  - 체크리스트
  - UI 변경 시 스크린샷
  - 이슈 종료 참조 (예: `Close #123`)

## CI/설정 노트
- CI는 JDK 21 사용, `.github/ci-gradle.properties` 적용.
- Gradle 설정은 Kotlin DSL (`*.gradle.kts`)로 유지.
