# Monorepo 운영 구조

## 경계

| 영역 | 책임 | 배포 경계 |
|---|---|---|
| `Backend` | OAuth, GitHub 수집, Snapshot, Analysis, Certificate, Verification API | Spring Boot 서비스 |
| `Frontend` | 로그인 화면, Dashboard, Certificate, 공개 검증 페이지 | Next.js 서비스 |
| `contracts` | Certificate Hash와 발급자/대상 지갑의 온체인 기록 | Foundry 배포 작업 |
| `docker-compose.yml` | 로컬 PostgreSQL | 로컬 개발 환경 |

## 의존 방향

```text
Frontend ──REST──> Backend ──> PostgreSQL
                       ├──────> GitHub API
                       ├──────> OpenAI API
                       └──────> Base Sepolia RPC
```

Frontend는 Blockchain RPC를 직접 다루지 않고, Wallet 서명 결과와 Transaction Hash만 Backend에 전달해. Backend는 Receipt와 온체인 상태를 확인하고 PostgreSQL에 저장해.

## GitHub 운영 방식

- `main`: 배포 가능한 상태
- 기능 작업: `feat/<short-name>`
- 버그 수정: `fix/<short-name>`
- PR 하나는 관련된 Backend/Frontend/contracts 변경을 함께 포함할 수 있음
- `.github/workflows/ci.yml`에서 세 영역을 독립적으로 검사
- Secret은 Repository Secret 또는 배포 플랫폼 Secret에만 저장
