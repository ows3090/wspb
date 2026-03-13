# Usage Guide

## 전제
이 저장소 기준 사용 시나리오는 "멀티모듈 내부 소비"입니다.  
(`sample-app`이 실제 예시)

## 1) 모듈/플러그인 적용
앱 모듈 `build.gradle.kts`에 다음을 설정합니다.

```kotlin
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.wspb.proto)
}

dependencies {
    implementation(project(":wspb-annotation"))
    ksp(project(":wspb-processor"))
}
```

## 2) 모델 선언
```kotlin
import com.wonseok.wspb.annotation.WSProto

@WSProto(name = "user_preference")
data class UserData(
    val id: Int,
    val name: String
)
```

## 3) KSP 옵션으로 패키지 커스터마이징 (선택)
옵션을 지정하지 않으면 기존 기본값이 사용됩니다.

```kotlin
ksp {
    arg("wspb.proto.packagePath", "proto/com/example/schema")
    arg("wspb.proto.javaPackage", "com.example.schema")
    arg("wspb.processor.verbose", "true")
}
```

- `wspb.proto.packagePath` 기본값: `proto/com/wonseok/wspb`
- `wspb.proto.javaPackage` 기본값: `com.wonseok.wspb`
- `wspb.processor.verbose` 기본값: `false`

## 4) 빌드
```bash
./gradlew :wspb-gradle-plugin:publishToMavenLocal --configure-on-demand
./gradlew :sample-app:assembleDebug
```

## 5) 생성 산출물 확인
아래 경로는 기본값 기준이며, `wspb.proto.packagePath` 설정 시 하위 경로가 달라질 수 있습니다.

- KSP 생성 proto:
  - `sample-app/build/generated/ksp/debug/resources/proto/com/wonseok/wspb/*.proto`
- Protobuf 생성 Java lite:
  - `sample-app/build/generated/sources/proto/debug/java/com/wonseok/wspb/*.java`

## 타입 매핑
- `Int`, `Short`, `Byte` -> `int32`
- `Long` -> `int64`
- `Float` -> `float`
- `Double` -> `double`
- `Boolean` -> `bool`
- `String` -> `string`
- `ByteArray` -> `bytes`
- `List<T>`, `Set<T>`, `Array<T>` -> `repeated <mapped(T)>`

## 네이밍 규칙
- 파일명: `@WSProto(name = "...")` 값 사용
- 메시지명: `snake_case` -> `PascalCase` 변환
- 필드명: 클래스 프로퍼티를 `snake_case`로 변환

## 제한사항
- 미지원 타입 사용 시 오류 발생
- 클래스명과 변환된 메시지명이 같으면 오류 발생
