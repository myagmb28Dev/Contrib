# GitHub Contribution Attestation Platform 개발 계획

## 1. 문서 목적

이 문서는 Temp_plan.md의 아이디어를 실제 개발 가능한 범위로 정리한 최종 개발 계획이다.

핵심 흐름은 다음과 같다.

> GitHub 활동 수집 → Contribution 분석 → Certificate 생성 → Hash 기록 → 공개 검증

초기 버전에서는 모든 기술을 한 번에 넣지 않고, 검증 가능한 Certificate를 만드는 핵심 흐름을 먼저 완성한다.

## 2. 최종 결정 사항

| 항목 | 결정 |
|---|---|
| 서비스 성격 | GitHub Contribution을 기반으로 한 검증 가능한 활동 증명 서비스 |
| 초기 대상 사용자 | 자신의 GitHub 활동을 포트폴리오로 보여주려는 개발자 |
| 초기 발급 모델 | 사용자가 직접 발급하는 Self-attested Certificate |
| 향후 발급 모델 | Repository Owner 또는 Maintainer가 발급하는 Attested Certificate |
| 최종 데이터베이스 | PostgreSQL |
| 초기 Blockchain | Base Sepolia |
| 원본 데이터 | GitHub API에서 수집한 Snapshot |
| 점수 기준 | 규칙 기반 계산 |
| AI 역할 | 점수 산정이 아닌 자연어 요약 및 영역 분류 |
| Blockchain 역할 | Certificate Hash와 발급 정보를 독립적으로 기록 |
| Private Key 보관 | 서버에 보관하지 않음 |
| 로컬 Git 분석 | V2 이후 |
| 다중 Blockchain | 초기 범위에서 제외 |

## 3. 서비스 정의

### 3.1 한 줄 설명

> GitHub Repository에서 관찰된 개발 활동을 Snapshot으로 고정하고, 이를 기반으로 생성한 Certificate를 누구나 검증할 수 있게 하는 서비스

### 3.2 서비스가 보장하는 것

- 특정 GitHub 계정과 Repository에 대해 수집된 활동 데이터를 기록한다.
- 어떤 기간과 분석 규칙으로 결과를 만들었는지 확인할 수 있다.
- Certificate의 원본 Payload가 발급 이후 변경되지 않았는지 확인할 수 있다.
- Blockchain에 기록된 Hash와 공개 Certificate를 비교할 수 있다.

다음은 보장하지 않는다.

- 개발자의 절대적인 실력
- 프로젝트 내 실제 영향력
- 기여의 품질에 대한 객관적인 평가
- 작성된 코드의 보안성 또는 적절성

화면과 Certificate에는 다음과 같은 설명을 사용한다.

> 이 Certificate는 선택한 Repository에서 관찰된 GitHub 활동을 기반으로 생성되었습니다.

## 4. 사용자 흐름

1. 사용자가 GitHub OAuth로 로그인한다.
2. Backend가 GitHub 사용자 ID와 계정 정보를 저장한다.
3. 사용자가 공개 Repository를 선택한다.
4. 사용자가 분석 기간을 선택한다.
5. Backend가 Commit, Pull Request, Review 데이터를 수집한다.
6. 수집 데이터로 Repository Snapshot을 만든다.
7. 규칙 기반 Contribution 지표와 Score를 계산한다.
8. 사용자가 Analysis 결과를 확인한다.
9. Certificate Preview를 확인하고 발급을 요청한다.
10. Certificate Payload와 Hash를 생성한다.
11. 사용자가 Wallet으로 Blockchain 트랜잭션에 서명한다.
12. Backend가 트랜잭션 상태와 Hash를 저장한다.
13. 누구나 공개 URL을 통해 Certificate를 검증한다.

## 5. MVP 범위

### 5.1 반드시 포함

- GitHub OAuth 로그인
- GitHub 사용자 정보 조회
- 공개 Repository 목록 조회
- Repository 하나 선택
- 분석 기간 지정
- Commit 수집
- Pull Request 수집
- Pull Request Review 수집
- Repository Snapshot 저장
- 규칙 기반 Contribution 지표 계산
- Contribution Score v1
- Analysis 결과 조회
- Certificate Preview
- Canonical JSON 생성
- Certificate Hash 생성
- 공개 Certificate 페이지
- Base Sepolia 기록
- Wallet 연결
- 공개 검증 API 및 페이지

### 5.2 초기 버전에서 제외

- Private Repository
- GitHub Webhook 기반 실시간 동기화
- 로컬 Git 저장소 분석
- Tauri 데스크톱 앱
- Issue 분석
- 다중 Blockchain
- NFT Marketplace
- Token 발행
- DAO
- Organization 관리자 시스템
- Maintainer 발급 흐름
- 고급 AI 평가
- Mainnet 발급

## 6. 시스템 아키텍처

~~~text
Next.js Frontend
    │
    │ REST API / 공개 검증 페이지
    ▼
Spring Boot Backend
    ├── Auth
    ├── GitHub Integration
    ├── Analysis Job
    ├── Contribution Analysis
    ├── Certificate
    ├── Blockchain Attestation
    └── Public Verification
    │
    ├── PostgreSQL
    ├── GitHub API
    ├── OpenAI API
    └── Base Sepolia
~~~

### 6.1 PostgreSQL

PostgreSQL을 최종 데이터베이스로 사용한다.

저장 대상:

- 사용자 및 GitHub 계정
- Repository 정보
- 분석 작업 상태
- 수집 Snapshot
- 활동 이벤트
- 분석 결과
- Certificate
- Blockchain 트랜잭션

PostgreSQL을 애플리케이션의 원본 데이터 저장소로 사용하며, Blockchain은 검증용 기록만 담당한다.

### 6.2 Redis

Redis는 초기 필수 구성 요소로 두지 않는다.

필요성이 확인된 이후 다음 용도로 추가한다.

- GitHub API 응답 캐시
- 분석 작업 Lock
- 중복 요청 방지
- 작업 큐 보조
- 짧은 TTL의 임시 데이터

분석 작업의 최종 상태와 Certificate 상태는 PostgreSQL에 저장한다.

### 6.3 비동기 작업

GitHub 데이터 수집과 AI 분석은 시간이 오래 걸릴 수 있으므로 비동기 Job으로 처리한다.

POST /api/analyses는 분석 결과를 즉시 반환하지 않고 Job ID를 반환한다.

작업 상태:

- PENDING
- COLLECTING
- ANALYZING
- AI_PROCESSING
- COMPLETED
- FAILED
- CANCELLED

## 7. Backend 모듈 구조

~~~text
com.example.project
├── auth
│   ├── controller
│   ├── service
│   ├── domain
│   └── repository
│
├── github
│   ├── client
│   ├── dto
│   ├── mapper
│   └── service
│
├── repository
│   ├── controller
│   ├── domain
│   ├── service
│   └── repository
│
├── analysis
│   ├── controller
│   ├── domain
│   ├── service
│   ├── collector
│   ├── calculator
│   └── repository
│
├── certificate
│   ├── controller
│   ├── domain
│   ├── payload
│   ├── hashing
│   ├── service
│   └── repository
│
├── blockchain
│   ├── controller
│   ├── client
│   ├── dto
│   └── service
│
├── verification
│   ├── controller
│   ├── service
│   └── dto
│
└── common
    ├── config
    ├── security
    ├── exception
    ├── response
    └── auditing
~~~

GitHub API DTO를 내부 Domain 객체가 직접 사용하지 않도록 Mapper 경계를 둔다.

## 8. 데이터 모델

### 8.1 User

- id
- createdAt
- updatedAt

### 8.2 GitHubAccount

- id
- userId
- githubUserId
- githubUsername
- email
- encryptedAccessToken
- tokenExpiresAt
- connectedAt
- revokedAt

Access Token은 평문으로 저장하지 않는다.

### 8.3 Repository

- id
- githubRepositoryId
- ownerGithubId
- ownerLogin
- name
- fullName
- url
- visibility
- defaultBranch
- language
- archived
- lastSyncedAt

Repository 식별은 변경될 수 있는 이름보다 GitHub의 숫자 ID를 기준으로 한다.

### 8.4 AnalysisJob

- id
- userId
- repositoryId
- periodStart
- periodEnd
- status
- progress
- errorCode
- errorMessage
- startedAt
- completedAt
- createdAt

같은 Repository, 사용자, 기간, Collector 버전에 대한 중복 분석을 방지한다.

### 8.5 RepositorySnapshot

- id
- analysisJobId
- repositoryId
- subjectGithubId
- periodStart
- periodEnd
- collectorVersion
- collectedAt
- sourceMetadata
- snapshotHash

Snapshot은 특정 시점의 분석 입력을 고정하는 핵심 엔티티다.

### 8.6 ActivityEvent

- id
- snapshotId
- externalId
- type
- authorGithubId
- occurredAt
- title
- state
- additions
- deletions
- changedFiles
- rawPayload

외부 API의 원본 데이터를 필요한 범위에서 보존한다.

### 8.7 ContributionAnalysis

- id
- snapshotId
- metrics
- score
- scoreVersion
- calculationRules
- technicalAreas
- aiSummary
- aiModel
- aiPromptVersion
- createdAt

규칙 기반 결과와 AI 결과를 구분해서 저장한다.

### 8.8 Certificate

- id
- publicId
- analysisId
- schemaVersion
- canonicalPayload
- certificateHash
- issuerWalletAddress
- subjectWalletAddress
- status
- issuedAt
- revokedAt
- revocationReason

Certificate는 발급 후 Payload를 변경하지 않는다.

### 8.9 BlockchainAttestation

- id
- certificateId
- chainId
- network
- contractAddress
- onchainCertificateId
- transactionHash
- blockNumber
- status
- submittedAt
- confirmedAt

## 9. GitHub 수집 규칙 v1

분석 결과를 재현하려면 수집 기준을 고정해야 한다.

- GitHub 사용자 식별은 username이 아니라 GitHub user ID를 사용한다.
- 시간은 서버 내부에서 UTC로 저장하고 화면에서 지역 시간으로 변환한다.
- Commit은 선택한 기간 안에서 subject가 author인 활동을 대상으로 한다.
- 기본 브랜치 기준 활동과 전체 Repository 활동의 범위를 명확히 저장한다.
- Merge Commit은 일반 Commit과 중복 집계하지 않는다.
- Pull Request 생성자와 Merge 상태를 별도로 기록한다.
- Review 제출 수와 Review 상태를 별도로 기록한다.
- Bot 계정은 기본적으로 제외한다.
- Co-authored Commit은 초기 버전에서 별도 기여로 집계하지 않는다.
- 추가·삭제 라인은 품질 점수가 아니라 활동량 보조 지표로 사용한다.
- 수집 데이터에는 collectorVersion을 기록한다.

## 10. Contribution Score v1

Score는 개발자의 능력을 평가하지 않고, 관찰된 활동을 요약하는 지표로 정의한다.

초기 구성 요소:

- Commit Activity
- Pull Request Activity
- Code Review
- Active Days
- Changed Files

각 지표는 원시 숫자를 그대로 더하지 않고 제한된 범위로 정규화한다.

점수 결과에는 반드시 다음 정보를 포함한다.

- 점수 값
- 점수 버전
- 산정 규칙
- 대상 기간
- 입력 Snapshot ID

예시:

~~~json
{
  "value": 82,
  "version": "v1",
  "components": {
    "commitActivity": 24,
    "pullRequestActivity": 28,
    "codeReview": 16,
    "activeDays": 9,
    "changedFiles": 5
  }
}
~~~

## 11. AI Analysis

AI는 다음 작업만 담당한다.

- 주요 기여 영역 요약
- 사용 기술 추정
- 작업 유형 분류
- 사람이 읽기 쉬운 Contribution Summary 생성

AI는 Score를 계산하지 않고, Certificate의 무결성 판단에도 사용하지 않는다.

AI 입력은 원본 GitHub 데이터 전체가 아니라 Snapshot에서 만든 제한된 구조화 데이터로 만든다.

AI 결과에는 다음 정보를 저장한다.

- 입력 Snapshot ID
- 사용 모델
- Prompt 버전
- 생성 시각
- 결과 JSON
- 재생성 횟수

Repository의 Commit 메시지와 PR 설명은 외부 입력으로 취급한다. AI 결과는 JSON Schema 검증을 통과해야 하며, AI 실패 시에도 규칙 기반 분석과 Certificate 발급은 동작해야 한다.

## 12. Certificate Payload와 Hash

Certificate는 화면용 데이터와 검증용 Payload를 분리한다.

검증용 Payload에는 다음 정보만 포함한다.

- Schema version
- Certificate ID
- GitHub user ID와 username
- Repository ID와 full name
- 분석 기간
- Snapshot ID 또는 Snapshot Hash
- Contribution metrics
- Score와 Score version
- 발급자
- 발급 시각

민감한 원본 GitHub 데이터나 Access Token은 Payload와 Blockchain에 포함하지 않는다.

### 12.1 Canonical JSON 규칙

- Key는 정해진 순서로 직렬화한다.
- 공백과 줄바꿈을 제거한다.
- 날짜는 UTC ISO-8601로 통일한다.
- 숫자 표현을 통일한다.
- Null 필드 처리 규칙을 고정한다.
- schemaVersion을 반드시 포함한다.

### 12.2 Hash 규칙

초기 EVM 기반 구현에서는 Ethereum 방식의 keccak256을 사용한다.

Hash는 화면, HTML, DB Row 전체가 아니라 canonical JSON에 대해 계산한다.

Java, TypeScript, Solidity에서 동일한 입력이 동일한 Hash를 생성하는 테스트 벡터를 유지한다.

## 13. Blockchain 설계

### 13.1 초기 네트워크

- 개발 및 테스트: Base Sepolia
- Mainnet: MVP 안정화 이후

### 13.2 Smart Contract 최소 기능

- Certificate 발급
- Certificate 조회
- Certificate 폐기
- Certificate Hash 저장
- Issuer 저장
- Subject Wallet 저장
- 발급·폐기 Event 기록

초기에는 NFT 표준을 사용하지 않는다.

### 13.3 발급 흐름

1. Backend가 Certificate Payload와 Hash를 생성한다.
2. Frontend가 사용자에게 내용을 보여준다.
3. 사용자가 Wallet을 연결한다.
4. 사용자가 발급 트랜잭션에 서명한다.
5. Frontend가 Transaction Hash를 Backend에 전달한다.
6. Backend가 Blockchain에서 Receipt를 확인한다.
7. 확인된 Chain 정보와 상태를 저장한다.

서버는 사용자의 Private Key를 저장하지 않는다.

### 13.4 발급자 모델

MVP:

- 사용자가 직접 발급
- issuer와 subject가 같을 수 있음
- 화면에 Self-attested 표시

V2:

- Repository Owner 또는 Maintainer가 발급
- Issuer 권한 검증
- 사용자와 발급자의 관계 표시

## 14. 공개 검증

공개 검증은 로그인 없이 가능해야 한다.

검증 순서:

1. Public Certificate ID 조회
2. 공개 Payload 조회
3. Payload의 Hash 재계산
4. Blockchain의 Hash 조회
5. 두 Hash 비교
6. Certificate 상태와 폐기 여부 확인
7. Transaction Explorer 링크 제공

DB는 편리한 조회를 위한 서비스 계층으로 사용한다.

사용자가 공개 Payload를 다운로드할 수 있도록 하여, 검증 페이지가 없어도 Payload와 Blockchain Hash를 비교할 수 있게 한다.

검증 결과:

- VALID
- HASH_MISMATCH
- REVOKED
- NOT_FOUND
- PENDING
- CHAIN_UNAVAILABLE

## 15. API 설계

### Authentication

~~~text
GET  /api/auth/github
GET  /api/auth/github/callback
POST /api/auth/logout
GET  /api/auth/me
~~~

### Repository

~~~text
GET /api/repositories
GET /api/repositories/{repositoryId}
~~~

### Analysis

~~~text
POST /api/analyses
GET  /api/analysis-jobs/{jobId}
GET  /api/analyses/{analysisId}
GET  /api/repositories/{repositoryId}/analyses
~~~

POST /api/analyses는 작업 생성 후 202 Accepted와 Job ID를 반환한다.

### Certificate

~~~text
POST /api/certificates/preview
POST /api/certificates
GET  /api/certificates
GET  /api/certificates/{certificateId}
POST /api/certificates/{certificateId}/revoke
~~~

### Blockchain

~~~text
POST /api/certificates/{certificateId}/attestation-intent
POST /api/certificates/{certificateId}/transaction
GET  /api/certificates/{certificateId}/transaction
~~~

### Public Verification

~~~text
GET /api/public/certificates/{publicId}
GET /api/public/certificates/{publicId}/payload
GET /api/public/certificates/{publicId}/verification
~~~

## 16. Frontend 페이지

~~~text
/
├── Landing
├── login
├── dashboard
│   ├── repositories
│   ├── analyses
│   └── certificates
├── repositories/{repositoryId}
│   ├── overview
│   ├── analyze
│   └── analysis/{analysisId}
├── certificates/{certificateId}
└── verify/{publicId}
~~~

공개 검증 페이지는 로그인 없이 접근할 수 있어야 한다.

## 17. 보안 및 개인정보

### GitHub

- OAuth state 검증
- 최소 권한 요청
- Access Token 암호화 저장
- Token 로그 출력 금지
- HttpOnly, Secure Cookie 사용
- 연결 해제 시 Token 폐기 및 삭제
- 사용자 데이터 삭제 요청 지원

### Backend

- 사용자별 Repository 접근 권한 확인
- 다른 사용자의 Analysis ID 접근 차단
- 분석 요청 Idempotency 보장
- 외부 API 응답 검증
- GitHub API Rate Limit 처리
- 민감 데이터의 예외 로그 마스킹

### Blockchain

- 원본 GitHub 데이터 저장 금지
- 이메일 등 개인정보의 온체인 기록 금지
- Private Key 서버 저장 금지
- Certificate 폐기 권한 제한
- Chain ID와 Contract Address 검증

## 18. 개발 단계와 완료 기준

### Phase 0: 설계 확정

- [ ] 발급자 모델 확정
- [ ] 수집 범위 확정
- [ ] Score v1 규칙 확정
- [ ] Snapshot 구조 확정
- [ ] Canonical JSON 규칙 확정
- [ ] Hash 알고리즘 확정
- [ ] 개인정보 공개 범위 확정

완료 기준:

> 개발 중 임의로 데이터 정의가 바뀌지 않도록 핵심 결정이 문서화되어 있다.

### Phase 1: Backend 기반과 인증

- [x] Spring Boot 프로젝트 생성
- [x] PostgreSQL 연결
- [x] Migration 구성
- [x] 공통 예외 응답 구성
- [x] GitHub OAuth 구현
- [x] User와 GitHubAccount 저장
- [x] 로그인·로그아웃·현재 사용자 조회

완료 기준:

> GitHub로 로그인하고 자신의 사용자 정보를 확인할 수 있다.

### Phase 2: GitHub 데이터 수집

- [x] Repository 목록 조회
- [x] Repository 저장
- [x] Commit 수집
- [x] Pull Request 수집
- [x] Review 수집
- [x] Pagination 처리
- [x] Rate Limit 처리
- [x] Rate Limit 자동 대기와 지수 백오프 재시도
- [x] RepositorySnapshot 저장
- [x] 중복 수집 방지

완료 기준:

> 같은 Repository와 기간에 대해 동일한 Snapshot을 재현할 수 있다.

### Phase 3: Contribution Analysis

- [x] ActivityEvent 정규화
- [x] 원시 metrics 계산
- [x] Score v1 계산
- [x] Score version 저장
- [x] Analysis 결과 저장
- [x] 분석 작업 재시도
- [x] DB Lease 기반 작업 복구와 재시작 후 재개
- [x] Dashboard 구현

완료 기준:

> Repository 하나를 선택하면 원시 지표와 Score 산정 근거를 확인할 수 있다.

### Phase 4: Certificate와 공개 검증

- [x] Certificate Payload 생성
- [x] Canonical JSON 구현
- [x] Hash 생성
- [x] Certificate Preview 구현
- [x] Certificate 발급 상태 관리
- [x] 공개 Certificate 페이지
- [x] Payload 다운로드
- [x] Hash 재계산 검증

완료 기준:

> 로그인하지 않은 제3자가 Certificate Payload의 무결성을 확인할 수 있다.

### Phase 5: Blockchain과 Wallet

- [x] Foundry 프로젝트 생성
- [x] Smart Contract 작성
- [x] Contract Unit Test
- [x] Base Sepolia 배포
- [x] Wallet 연결
- [x] 발급 트랜잭션 요청
- [x] Receipt 확인
- [x] Transaction 정보 저장
- [x] On-chain Hash 검증
- [x] Receipt 백그라운드 조정과 UI 폴링
- [x] On-chain Certificate 폐기
- [x] GitHub 연결부터 공개 검증·폐기까지 자동 E2E 테스트

완료 기준:

> 사용자가 자신의 Wallet으로 Self-attested Certificate를 Base Sepolia에 기록하고, 공개 페이지에서 검증할 수 있다.

### Phase 6: AI Summary

- [x] Snapshot 기반 AI 입력 구조 생성
- [x] Prompt 버전 관리
- [x] Structured Output 검증
- [x] Summary 생성
- [x] Technical Area 생성
- [x] 결과 재생성
- [x] AI 실패 시 Fallback

완료 기준:

> AI 결과가 규칙 기반 지표와 분리되어 표시되고, AI 실패가 Certificate 발급을 막지 않는다.

### Phase 7: 배포와 운영

- [ ] Frontend 배포
- [ ] Backend 배포
- [ ] PostgreSQL 운영 환경 구성
- [ ] HTTPS
- [ ] Production OAuth 설정
- [x] Container 배포 Artifact와 운영 환경 검증
- [x] Secret 관리 절차와 필수값 검증
- [x] Backup·검증·보존 스크립트
- [x] ECS JSON Structured Logging
- [x] Health Probe, Prometheus Metrics, Alert 규칙
- [x] Request ID 기반 Error Tracking

완료 기준:

> 실제 운영 환경에서 분석 요청부터 공개 검증까지 전체 흐름이 동작한다.

## 19. 테스트 계획

### Backend

- Domain 계산 단위 테스트
- Score 규칙 테스트
- Canonical JSON 테스트
- Hash 테스트
- OAuth 상태 검증 테스트
- 사용자 권한 테스트
- API 응답 테스트

### GitHub Integration

- Pagination 테스트
- Rate Limit 테스트
- API 실패 및 재시도 테스트
- 중복 수집 테스트
- Fixture 기반 Snapshot 재현 테스트

### Blockchain

- 발급 테스트
- 폐기 테스트
- 권한 테스트
- 중복 Certificate 테스트
- Event 검증 테스트
- Base Sepolia 통합 테스트

### Cross-runtime Hash

동일한 Payload에 대해 다음 환경에서 Hash가 같은지 검증한다.

- Java
- TypeScript
- Solidity

## 20. 운영 원칙

1. PostgreSQL을 최종 데이터베이스로 사용한다.
2. GitHub 원본 데이터와 AI 생성 결과를 분리한다.
3. Snapshot을 기준으로 분석을 재현한다.
4. Score는 개발자의 절대적인 실력으로 표현하지 않는다.
5. Certificate Payload는 발급 후 변경하지 않는다.
6. Blockchain에는 검증에 필요한 최소 데이터만 기록한다.
7. Wallet Private Key를 서버에 저장하지 않는다.
8. AI는 설명 레이어로만 사용한다.
9. 분석 작업은 Idempotent하게 만든다.
10. 테스트넷에서 검증한 뒤 Mainnet으로 확장한다.

## 21. 최종 성공 기준

다음 시나리오가 모두 성공하면 첫 번째 완성 버전으로 본다.

1. 사용자가 GitHub로 로그인한다.
2. Repository를 선택한다.
3. 분석 기간을 지정한다.
4. GitHub 활동 수집 작업이 완료된다.
5. 원시 Contribution 지표와 Score가 표시된다.
6. Certificate Preview가 생성된다.
7. 사용자가 Wallet으로 발급을 승인한다.
8. Base Sepolia에 Hash가 기록된다.
9. 공개 URL을 다른 사람에게 공유한다.
10. 로그인하지 않은 사용자가 Certificate를 검증한다.
11. Payload Hash와 On-chain Hash가 일치한다.
12. Certificate를 폐기하면 검증 결과가 REVOKED로 바뀐다.
