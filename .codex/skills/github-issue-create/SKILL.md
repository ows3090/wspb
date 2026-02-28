---
name: github-issue-create
description: GitHub 저장소에 이슈를 생성하거나(필요 시) 수정할 때 사용하는 표준 워크플로우
---

# GitHub Issue Create 스킬

## 목적
- GitHub 이슈를 일관된 양식으로 작성한다.

## 기본 원칙
- 기본 동작은 **이슈 생성**이다.
- 사용자 요청이 없으면 기존 이슈 수정/종료는 하지 않는다.
- 이슈 할당자는 항상 내 자신으로 한다.
- 라벨은 새롭게 생성하지 않고, 생성되어 있는 라벨(feature, doc, bugfix) 중 하나를 선택한다.
- 이슈는 제목, 내용 항상 영어로 작성한다.

## 입력 체크리스트

1. 필수
- `owner`
- `repo`
- `title`
- `body`

2. 선택
- `labels` (문자열 배열)
- `assignees` (GitHub username 배열)

## 실행 순서

1. 사용자 요청에서 `owner/repo/title/body`를 추출한다.
2. 누락 필드가 있으면 최소 질문으로 보완한다.
3. 중복 이슈 가능성이 높으면 검색을 제안한다(필수 아님).
4. 아래 호출로 생성한다.
- `mcp__github__issue_write`
- `method: "create"`
- `owner`, `repo`, `title`, `body`, `labels`, `assignees`, `milestone`, `type`

## 실패 처리 규칙

- 권한 오류: 권한 부족을 명시하고 권한 있는 사용자/토큰 확인 안내
- 검증 오류: 누락/형식 오류 필드를 명시하고 재입력 요청
- 네트워크/일시 오류: 동일 파라미터로 1회 재시도 제안

## 출력 형식

생성 성공 시 아래를 보고한다:
- 이슈 번호: `#<number>`
- URL
- 제목
- 적용된 `labels`, `assignees` 요약

예시:
- Created issue `#123`
- URL: `https://github.com/<owner>/<repo>/issues/123`
- Labels: `bug`, `android`
- Assignees: `alice`

## 본문 템플릿(권장)

```md
## 배경
...

## 문제
...

## 개발(수정) 계획
- 
- 

## 참고
...

