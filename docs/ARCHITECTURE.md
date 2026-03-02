# Architecture

## 목적
WSPB는 Android 프로젝트에서 Kotlin 모델 선언을 기준으로 Protobuf 스키마/코드를 자동 생성하기 위한 멀티모듈 도구 체인입니다.

## 모듈 경계
- `wspb-annotation`
  - 외부 API 표면
  - `@WSProto(name = "...")` 정의
- `wspb-processor`
  - KSP 기반 생성 엔진
  - `@WSProto` 클래스에서 `.proto` 생성
- `wspb-gradle-plugin`
  - 빌드 통합 계층
  - Protobuf plugin 설정 + variant별 sourceSet 연결
- `sample-app`
  - 사용자 사용 시나리오 검증 앱
  - 라이브러리 본체 아님

## 의존 방향
`sample-app` -> `wspb-annotation`  
`sample-app` -> `wspb-processor` (ksp)  
`sample-app` -> `wspb-gradle-plugin` (`wspb.proto`)

`wspb-processor` -> `wspb-annotation`  
`wspb-gradle-plugin`은 annotation/processor에 직접 의존하지 않고 빌드 파이프라인만 관리

## 빌드 파이프라인
1. 사용자가 클래스에 `@WSProto(name = "...")` 선언
2. KSP가 `.proto` 생성
   - 경로: `build/generated/ksp/<variant>/resources/proto/com/wonseok/wspb`
3. Gradle plugin이 위 경로를 Android variant proto sourceSet으로 등록
4. Protobuf plugin이 `protoc`로 Java lite 소스 생성
   - 경로: `build/generated/sources/proto/<variant>/java`
5. Android 컴파일 단계에서 생성 소스 포함

## 설계 의도
- 사용자 API(애노테이션)와 생성 구현(KSP), 빌드 통합(Gradle Plugin)을 분리해 변경 영향 범위를 축소
- 소비자는 앱 코드에서 애노테이션만 사용하고, 빌드 연결은 플러그인에 위임

## 현재 제약
- 타입 지원 범위 제한(기본 타입 + `List/Set/Array`)
- `.proto` 패키지/옵션 커스터마이징 제한
- KSP 로그가 기본적으로 verbose
- 로컬 개발 시 `mavenLocal` 플러그인 해석에 영향을 받을 수 있음

## 개선 우선순위 제안
1. 프로세서 타입 지원 확장(nullable, enum, custom type)
2. 플러그인 해석/개발 흐름 단순화(included build 전략 정리)
3. 테스트 자동화 보강(golden test + plugin functional test)
