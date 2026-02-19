---
name: git-commit
description: 변경사항 분석 후 작업 단위로 커밋
---

# Git Commit 스킬

## 실행 순서

1. `git status`와 `git diff`로 전체 변경사항 파악
2. `git log --oneline -10`으로 최근 커밋 스타일 확인
3. **보안 검사** 실행 (시크릿, 민감 파일 체크)
4. **불필요 파일 검사** (.gitignore 추가 대상 확인)
5. 변경사항을 **작업 단위로 분류** (하나의 커밋 = 하나의 작업)
6. 각 커밋별로 관련 파일만 `git add`하여 **개별 커밋**
7. 커밋 완료 후 `git status`로 결과 확인

## 커밋 메시지 형식

```
<type>: <설명>
```

### Type 규칙

| Type | 사용 시점 |
|------|----------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `refactor` | 코드 개선, 정리, 삭제, 스타일 변경 |
| `docs` | 문서 추가/수정 |
| `chore` | 빌드 설정, 의존성, 기타 |

### 작성 규칙

- 한글로 작성
- 50자 이내, 마침표 없음
- 명령형으로 작성 ("추가", "수정", "삭제" 등)
- 무엇을 했는지가 아닌 **왜** 했는지 중심

### 예시

```
feat: 열차 조회 시간범위 필터 추가
fix: NetFunnel 서브코드 200 판별 누락 수정
refactor: UseCase 레이어 중복 로그 제거
docs: README에 API 명세서 추가
chore: ProGuard 규칙에 Room 예외 추가
```

## 커밋 분리 기준

- **다른 type**이면 분리 (feat + fix → 2개 커밋)
- **같은 type**이면 웬만하면 하나로 묶음
- 관련 파일은 하나의 커밋으로

## 보안 검사

커밋 전 변경사항에서 다음 패턴을 반드시 확인한다:

### 커밋 금지 파일

- `.env`, `.env.*`
- `*.keystore`, `*.jks`
- `google-services.json`
- `local.properties`
- `credentials.json`, `secrets.json`
- `*.pem`, `*.p12`, `*.key`

### 코드 내 시크릿 탐지

변경된 코드에서 다음 패턴이 있으면 **커밋 중단하고 사용자에게 경고**:

- API 키: `api_key`, `apiKey`, `API_KEY` 등에 하드코딩된 값
- 토큰: `token = "..."`, `bearer ...`
- 비밀번호: `password = "..."`, `pwd = "..."`
- 시크릿: `secret = "..."`, `client_secret`
- Base64로 인코딩된 긴 문자열 (64자 이상)

발견 시: 커밋하지 않고 "시크릿이 포함된 파일이 있습니다: [파일명]" 경고

## 불필요 파일 검사

`git status`에서 다음 패턴의 파일이 발견되면 **커밋하지 않고 .gitignore에 추가**:

### 자동 감지 대상

- 빌드 산출물: `build/`, `*.apk`, `*.aab`, `*.class`
- IDE 설정: `.idea/`, `*.iml`, `.vscode/`
- OS 파일: `.DS_Store`, `Thumbs.db`, `._*`
- 로그/캐시: `*.log`, `*.hprof`, `.gradle/`
- 의존성: `node_modules/`, `.cxx/`

### 처리 방식

1. 해당 파일이 이미 `.gitignore`에 있는지 확인
2. 없으면 `.gitignore`에 패턴 추가
3. 이미 tracked 중이면 `git rm --cached`로 추적 해제
4. .gitignore 변경은 별도 `chore` 커밋으로 먼저 커밋

## 금지사항

- `git add -A` 또는 `git add .` 사용 금지 (파일 단위로 add)
- 빈 커밋 금지
- `--amend` 사용 금지 (새 커밋 생성)
- `--no-verify` 사용 금지
