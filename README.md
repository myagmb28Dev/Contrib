# GitHub Contribution Attestation Platform

GitHub Repository에서 수집한 개발 활동을 Snapshot으로 고정하고, 규칙 기반 분석 결과를 검증 가능한 Certificate로 발급하는 플랫폼이다.

```text
GitHub OAuth
    → Activity Collection
    → Repository Snapshot
    → Contribution Analysis
    → Certificate Payload / Hash
    → Base Sepolia Attestation
    → Public Verification
```

## 주요 기능

- GitHub OAuth 인증 및 공개 Repository 조회
- Commit, Pull Request, Review 활동 수집
- 재현 가능한 Repository Snapshot 생성
- 규칙 기반 Contribution Score 산정
- AI 기반 기여 요약 및 기술 영역 분류
- Canonical JSON과 Keccak-256 기반 Certificate Hash 생성
- Base Sepolia를 통한 Self-attested Certificate 기록
- 로그인 없는 Certificate 공개 검증

## 기술 스택

| 영역 | 기술 |
|---|---|
| Backend | Java 17, Spring Boot 3.5, Gradle 8 |
| Frontend | Next.js 16, React 19, TypeScript |
| Database | PostgreSQL |
| Blockchain | Solidity 0.8.24, Foundry, Base Sepolia |
| CI | GitHub Actions |

## 저장소 구조

```text
.
├── Backend/       # REST API, GitHub 수집, 분석, Certificate, 공개 검증
├── Frontend/      # Next.js App Router 기반 웹 애플리케이션
├── contracts/     # Certificate Attestation 스마트 컨트랙트
├── docs/          # 모노레포 운영 문서
└── .github/       # GitHub Actions 워크플로
```

각 영역은 하나의 저장소에서 버전을 공유하며 독립적으로 빌드하고 검증한다.

## 개발 환경

- JDK 17 이상
- Node.js 20.9 이상
- Foundry: 스마트 컨트랙트 개발 시 필요

### Backend

```powershell
cd Backend
.\gradlew.bat bootRun
```

- 기본 포트: `8080`
- Health Check: `GET http://localhost:8080/api/health`
- 테스트: `.\gradlew.bat test`

### Frontend

```powershell
cd Frontend
npm ci
npm run dev
```

- 기본 포트: `3000`
- 정적 검사: `npm run lint`
- 타입 검사: `npm run typecheck`
- 프로덕션 빌드: `npm run build`

### Contracts

```powershell
cd contracts
forge build
forge test
```

## 환경 변수

루트의 `.env.example`을 기준으로 로컬 환경 파일을 구성한다.

```powershell
Copy-Item .env.example .env
```

| 변수 | 용도 |
|---|---|
| `DATABASE_URL` | PostgreSQL JDBC URL |
| `POSTGRES_USER` | PostgreSQL 사용자 |
| `POSTGRES_PASSWORD` | PostgreSQL 비밀번호 |
| `GITHUB_CLIENT_ID` | GitHub OAuth Client ID |
| `GITHUB_CLIENT_SECRET` | GitHub OAuth Client Secret |
| `OPENAI_API_KEY` | AI Summary API 인증 |
| `NEXT_PUBLIC_API_BASE_URL` | Frontend의 Backend API 기준 URL |
| `BASE_SEPOLIA_RPC_URL` | Base Sepolia RPC Endpoint |
| `ATTESTATION_CONTRACT_ADDRESS` | 배포된 Attestation Contract 주소 |

실제 Secret은 저장소에 커밋하지 않으며 GitHub Actions 또는 배포 환경의 Secret 저장소에서 관리한다.

## CI

`.github/workflows/ci.yml`은 `main` Push와 Pull Request에서 다음 Job을 병렬 실행한다.

- Backend: Gradle Test
- Frontend: Install, Lint, Type Check, Build
- Contracts: Foundry Build, Test

## 설계 원칙

- 분석 입력은 Snapshot으로 고정하고 재현 가능해야 한다.
- 규칙 기반 Score와 AI 생성 결과를 분리한다.
- Certificate Payload는 발급 후 변경하지 않는다.
- Blockchain에는 검증에 필요한 최소 데이터만 기록한다.
- Wallet Private Key는 서버에 저장하지 않는다.
- 공개 검증은 인증 없이 접근할 수 있어야 한다.

## 관련 문서

- [개발 계획](Backend/docs/Project_plan.md)
- [모노레포 운영 구조](docs/monorepo.md)
