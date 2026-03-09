# Troubleshooting

## 1) `Internal schema project ... was not found`
에러 예:
`[WSPB] Internal schema project ':__wspb_schema__sample-app' was not found ...`

대응:
1. 플러그인 로컬 게시 실행
   - `./gradlew :wspb-gradle-plugin:publishToMavenLocal --configure-on-demand`
2. 다시 빌드
   - `./gradlew :sample-app:assembleDebug`

원인:
- 로컬에 해석된 플러그인 버전/상태가 현재 소스와 다를 때 발생할 수 있습니다.

## 2) `.proto`가 생성되지 않음
점검:
1. 모델 클래스에 `@WSProto`가 붙어 있는지 확인
2. 앱 모듈에 `ksp(project(":wspb-processor"))` 의존성이 있는지 확인
3. 빌드 후 아래 경로 확인
   - `build/generated/ksp/<variant>/resources/proto/...`

## 3) unsupported type 오류
원인:
- 현재 프로세서가 지원하지 않는 타입 사용

대응:
- 지원 타입으로 변경하거나, 프로세서 타입 매핑 로직 확장
- 지원 타입 목록: `docs/USAGE.md`

## 4) Protobuf Java 코드가 보이지 않음
점검:
1. `wspb.proto` 플러그인이 적용됐는지 확인
2. `generate<Variant>Proto` 태스크가 실행됐는지 확인
3. 출력 경로 확인
   - `build/generated/sources/proto/<variant>/java`

## 5) 빌드는 되는데 로그가 과도함
원인:
- KSP 프로세서가 `logger.warn(...)`를 다수 출력

대응:
- `ksp { arg("wspb.processor.verbose", "false") }`로 비활성화 (기본값도 `false`)

## 6) 커스텀 패키지 옵션이 반영되지 않음
점검:
1. 앱 모듈에 `ksp` 블록이 선언되어 있는지 확인
2. 옵션 키 오탈자 확인
   - `wspb.proto.packagePath`
   - `wspb.proto.javaPackage`
3. 값이 공백 문자열인지 확인

원인:
- 공백 문자열 또는 잘못된 값이면 프로세서가 기본값으로 fallback합니다.

대응:
- 유효한 문자열/불리언 값으로 다시 설정 후 빌드합니다.
