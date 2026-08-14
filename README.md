# GitHub Contribution Attestation Platform

GitHub 활동을 기간별 Snapshot으로 고정하고, 규칙 기반 Contribution 분석 결과를 Certificate로 발급한 뒤 누구나 무결성을 검증할 수 있게 하는 모노레포야.

핵심 흐름은 `GitHub OAuth → 활동 수집 → Snapshot → Score/AI 요약 → Certificate → Base Sepolia → 공개 검증`이야.

## 저장소 구조

```text
.
├── Backend/       # Spring Boot REST API, 수집/분석/Certificate/검증
├── Frontend/      # Next.js App Router 웹 애플리케이션
├── contracts/     # Foundry 기반 Base Sepolia 스마트 컨트랙트
├── docs/          # 모노레포 운영 문서
└── .github/       # GitHub Actions와 저장소 자동화
```

세부 제품 요구사항은 [Backend/docs/Project_plan.md](Backend/docs/Project_plan.md)에 있어.

## 로컬 준비물

- JDK 17 이상
- Node.js 20.9 이상
- Foundry: 컨트랙트 작업을 할 때만 필요

## 시작하기

### 1. Backend

```powershell
cd Backend
.\gradlew.bat bootRun
```

헬스 체크: `http://localhost:8080/api/health`

### 2. Frontend

```powershell
cd Frontend
npm install
npm run dev
```

웹: `http://localhost:3000`

### 3. Contracts

```powershell
cd contracts
forge build
forge test
```

## 환경 변수

실제 Secret은 커밋하지 말고 루트의 `.env.example`을 복사해 로컬 환경에 맞게 채워.

```powershell
Copy-Item .env.example .env
```

GitHub OAuth Client Secret, OpenAI API Key, RPC URL, 지갑 정보는 GitHub Actions Secret 또는 배포 플랫폼 Secret으로만 관리해.

## GitHub에 올리기

원격 저장소를 만든 뒤 루트에서 실행하면 돼.

```powershell
git init
git branch -M main
git add .
git commit -m "chore: initialize contribution attestation monorepo"
git remote add origin https://github.com/<owner>/<repository>.git
git push -u origin main
```

이 저장소는 Backend, Frontend, contracts를 하나의 변경 단위로 관리하지만, CI는 각 영역을 독립 Job으로 실행해. 그래서 한 저장소 안에서 같이 버전 관리하면서도 배포 경계는 분리할 수 있어.

## 개발 순서

1. Phase 0~1: Backend 기반, PostgreSQL, GitHub OAuth
2. Phase 2: Repository와 GitHub 활동 Snapshot 수집
3. Phase 3: 규칙 기반 Score와 Dashboard
4. Phase 4: Canonical JSON, Hash, 공개 검증
5. Phase 5: Wallet과 Base Sepolia
6. Phase 6~7: AI 요약, 배포와 운영

구현 중에도 계획서의 원칙인 `Snapshot 재현성`, `Score와 AI 결과 분리`, `Private Key 서버 미보관`, `Blockchain 최소 데이터 기록`을 유지해.
