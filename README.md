# wspb

Kotlin 모델 클래스에 `@WSProto`를 선언하면, KSP와 Gradle Plugin을 통해 `.proto`와 Protobuf lite 코드를 자동 생성하는 Android 도구 체인입니다.

## 핵심 포인트
- `sample-app`은 라이브러리 본체가 아닌 "사용 예시 앱"입니다.
- 라이브러리 핵심 모듈은 `wspb-annotation`, `wspb-processor`, `wspb-gradle-plugin`입니다.
- 빌드 파이프라인: `Annotation -> KSP proto 생성 -> sourceSet 연결 -> protoc(java lite) 생성`

## 모듈 구성
- `wspb-annotation`: `@WSProto` 애노테이션 제공
- `wspb-processor`: KSP SymbolProcessor로 `.proto` 생성
- `wspb-gradle-plugin`: `wspb.proto` 플러그인(Proto sourceSet 연결 + Protobuf 설정)
- `sample-app`: 실제 소비자 관점 통합 예시

## 빠른 시작 (로컬 개발)
현재 저장소 기준으로는 플러그인 로컬 게시 후 샘플 앱을 빌드하는 순서가 가장 안전합니다.

```bash
./gradlew :wspb-gradle-plugin:publishToMavenLocal --configure-on-demand
./gradlew :sample-app:assembleDebug
```

## 사용 예시
```kotlin
@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String
)
```

생성 결과(예):
- `build/generated/ksp/debug/resources/proto/com/wonseok/wspb/user_preference.proto`
- `build/generated/sources/proto/debug/java/com/wonseok/wspb/UserPreference.java`

## 문서 인덱스
- [아키텍처](docs/ARCHITECTURE.md)
- [사용 가이드](docs/USAGE.md)
- [개발 가이드](docs/DEVELOPMENT.md)
- [문제 해결](docs/TROUBLESHOOTING.md)
- [기여 가이드](CONTRIBUTING.md)
- [에이전트 운영 가이드](AGENTS.md)

## CI
GitHub Actions는 다음 순서로 검증합니다.
1. `:wspb-gradle-plugin:publishToMavenLocal --configure-on-demand`
2. `spotlessCheck`
3. `lint`
4. `:sample-app:assemble`
