# GitHub Contribution On-Chain Certificate

## 1. 프로젝트 개요

GitHub 개발 활동을 분석해 개발자의 프로젝트 기여도를 정리하고, 해당
결과를 블록체인 기반의 검증 가능한 Certificate로 발급하는 웹
애플리케이션을 만든다.

핵심 방향은 단순히 "GitHub 활동량을 보여주는 서비스"가 아니라 다음
흐름을 하나의 서비스로 연결하는 것이다.

> GitHub 활동 수집 → Contribution 분석 → Certificate 생성 → 블록체인
> 기록 → 공개 검증

초기 버전에서는 **GitHub 데이터만 분석**한다.\
로컬 Git 저장소 분석은 향후 V2 확장 기능으로 남겨둔다.

------------------------------------------------------------------------

## 2. 프로젝트 목표

### 핵심 목표

-   GitHub OAuth를 통한 간편한 사용자 인증
-   사용자가 원하는 Repository를 선택해 기여 데이터 수집
-   Commit / Pull Request / Issue / Review 등의 활동 분석
-   프로젝트별 Contribution Profile 생성
-   분석 결과를 기반으로 Certificate 발급
-   블록체인에 Certificate의 검증 정보를 기록
-   누구나 공개 URL을 통해 Certificate의 진위를 검증할 수 있도록 구현

### 프로젝트에서 보여주고 싶은 기술 역량

-   Spring Boot 기반 REST API 설계
-   외부 API(GitHub) 연동
-   OAuth 인증 및 권한 관리
-   PostgreSQL 데이터 모델링
-   Redis 캐싱 및 비동기 작업 처리
-   AI API 연동
-   Solidity Smart Contract 개발
-   Wallet 연결 및 블록체인 트랜잭션
-   Web3 데이터 검증
-   Next.js 기반 웹 애플리케이션 구현
-   Docker / CI/CD

------------------------------------------------------------------------

## 3. 서비스 컨셉

### 한 줄 설명

> GitHub 개발 활동을 검증 가능한 온체인 경력 증명으로 만들어주는 서비스

### 사용자 관점의 흐름

1.  GitHub 계정으로 로그인
2.  Repository 선택
3.  GitHub 활동 데이터 수집
4.  Contribution 분석
5.  분석 결과 확인
6.  Wallet 연결
7.  Certificate 발급
8.  블록체인 기록
9.  공개 검증 URL 공유

------------------------------------------------------------------------

# 4. 기술 스택

## Frontend

### Next.js

-   TypeScript
-   App Router
-   Server-side rendering이 필요한 공개 페이지 지원
-   Dashboard 및 인증서 화면 구현

### UI

-   Tailwind CSS
-   shadcn/ui

------------------------------------------------------------------------

## Backend

### Spring Boot

주요 역할:

-   사용자 인증 및 권한 관리
-   GitHub OAuth 처리
-   GitHub API 연동
-   Contribution 데이터 수집
-   Contribution 분석
-   AI API 연동
-   Certificate 관리
-   Blockchain 관련 API
-   공개 검증 API
-   비동기 작업 관리

------------------------------------------------------------------------

## Database

### PostgreSQL

저장 대상:

-   사용자
-   GitHub 계정
-   Repository
-   Contribution 데이터
-   분석 결과
-   Certificate
-   Blockchain Transaction
-   Wallet 정보

------------------------------------------------------------------------

## Cache / Async

### Redis

초기 활용:

-   GitHub API 응답 캐싱
-   분석 작업 상태 관리
-   중복 요청 방지
-   임시 데이터 저장

필요성이 확인되면 이후 작업 큐 구조로 확장한다.

------------------------------------------------------------------------

## GitHub

### GitHub OAuth

사용자 GitHub 계정 인증 및 Repository 접근 권한 획득.

### GitHub API

가능하면 GraphQL API를 중심으로 사용하고, 필요한 기능에 따라 REST API를
병행한다.

수집 대상 예시:

-   Repository
-   Commit
-   Pull Request
-   Issue
-   Pull Request Review
-   Contributor 정보
-   변경 파일 및 변경량

------------------------------------------------------------------------

## AI

### OpenAI API

AI는 단순 활동량 계산이 아니라 수집된 데이터를 기반으로 Contribution을
설명하고 분류하는 데 사용한다.

예시:

-   주요 기여 영역
-   사용 기술
-   작업 유형
-   프로젝트 내 역할
-   Contribution Summary

AI 결과는 절대적인 실력 평가가 아니라 **관찰된 개발 활동에 대한 요약 및
분석**으로 취급한다.

------------------------------------------------------------------------

## Blockchain

### Network

초기 개발:

> Base Sepolia

서비스 안정화 후:

> Base Mainnet

### Smart Contract

-   Solidity
-   Foundry

### Web3 Client

-   viem

### Wallet

-   MetaMask
-   WalletConnect 계열 지원 고려

------------------------------------------------------------------------

## Infrastructure

-   Docker
-   GitHub Actions
-   Vercel: Frontend
-   AWS: Backend / Database / Redis 등

배포 환경은 개발 과정에서 단순화할 수 있으며, 초기에는 하나의 서버에서
여러 컴포넌트를 운영하는 방식도 허용한다.

------------------------------------------------------------------------

# 5. 전체 아키텍처

``` text
                         ┌──────────────────────┐
                         │       User           │
                         └──────────┬───────────┘
                                    │
                                    ▼
                         ┌──────────────────────┐
                         │       Next.js        │
                         │      Frontend        │
                         │                      │
                         │ Dashboard            │
                         │ Profile              │
                         │ Certificate          │
                         │ Verification         │
                         │ Wallet Connection    │
                         └──────────┬───────────┘
                                    │ REST API
                                    ▼
                         ┌──────────────────────┐
                         │    Spring Boot       │
                         │       Backend        │
                         │                      │
                         │ Auth                 │
                         │ GitHub Integration   │
                         │ Contribution         │
                         │ AI Analysis          │
                         │ Certificate          │
                         │ Blockchain           │
                         └───────┬───────┬──────┘
                                 │       │
                    ┌────────────┘       └─────────────┐
                    ▼                                  ▼
             ┌──────────────┐                   ┌──────────────┐
             │ PostgreSQL   │                   │    Redis     │
             └──────────────┘                   └──────────────┘
                    │
                    │
                    ▼
             ┌──────────────┐
             │  GitHub API  │
             └──────────────┘

                         Spring Boot
                              │
                              ▼
                       ┌─────────────┐
                       │ OpenAI API  │
                       └─────────────┘

                         Certificate
                              │
                              ▼
                       ┌─────────────┐
                       │ viem / Web3 │
                       └──────┬──────┘
                              ▼
                     ┌─────────────────┐
                     │ Smart Contract  │
                     │    Solidity     │
                     └────────┬────────┘
                              ▼
                       Base Blockchain
```

------------------------------------------------------------------------

# 6. 서비스 주요 기능

## 6.1 GitHub 로그인

### 목표

GitHub 계정을 이용해 사용자를 인증한다.

### 기능

-   GitHub OAuth 로그인
-   GitHub 사용자 정보 조회
-   Access Token 안전한 저장
-   연결 해제
-   로그아웃

### 고려사항

GitHub Token은 프론트엔드에 장기간 노출하지 않는다.

------------------------------------------------------------------------

# 7. Repository 연결

사용자가 자신의 GitHub Repository 중 하나를 선택한다.

### 화면 예시

``` text
My Repositories

┌─────────────────────────────┐
│ pogun                       │
│ Spring Boot                 │
│ 142 commits                 │
│                             │
│ [Analyze]                   │
└─────────────────────────────┘

┌─────────────────────────────┐
│ windexbar                   │
│ WinUI                       │
│ 87 commits                  │
│                             │
│ [Analyze]                   │
└─────────────────────────────┘
```

------------------------------------------------------------------------

# 8. Contribution 데이터 수집

초기 분석 대상:

-   Commit
-   Pull Request
-   Issue
-   Review
-   Repository
-   변경 파일
-   추가/삭제 라인
-   활동 기간

단순히 개수만 저장하지 않고 원본 데이터를 기반으로 분석할 수 있도록
설계한다.

------------------------------------------------------------------------

# 9. Contribution 분석

수집된 데이터를 기반으로 프로젝트 내 기여도를 분석한다.

예시:

``` text
Contribution Summary

Commits             438
Pull Requests        31
Reviews              18
Issues               12

Main Contribution
Backend Development

Technical Areas
- Authentication
- API Development
- Database
- Infrastructure
```

------------------------------------------------------------------------

## Contribution Score

초기에는 명확한 수식 기반 점수를 사용하고, AI 결과와 분리한다.

예:

``` text
Contribution Score

Commit Activity       25
Pull Requests         25
Code Review           15
Issue Contribution    10
Project Duration      10
Code Changes          15

Total                 100
```

점수는 "개발 실력"이 아니라 **해당 Repository에서 관찰된 기여 활동의
지표**로 정의한다.

------------------------------------------------------------------------

# 10. AI Analysis

AI는 Contribution 데이터를 읽고 자연어 기반 분석을 생성한다.

### 입력 예시

``` text
Repository:
example-project

Contributor:
user

Commits:
438

Pull Requests:
31

Reviews:
18

Changed Areas:
authentication
payment
database
```

### 출력 예시

``` text
Contribution Summary

The contributor primarily worked on backend
architecture and authentication-related features.

Major contribution areas:
- Authentication
- API development
- Database

Observed role:
Backend-focused contributor
```

AI 분석 결과에는 원본 GitHub 데이터와 AI가 생성한 내용을 명확하게
구분한다.

------------------------------------------------------------------------

# 11. Certificate

분석이 완료되면 사용자는 Contribution Certificate를 생성할 수 있다.

### Certificate 예시

``` text
Contribution Certificate

Project
Pogun

Contributor
GitHub Username

Role
Backend Developer

Contribution Period
2026.01 - 2026.07

Contribution Score
87 / 100

Major Areas
Backend
Authentication
Database

Certificate ID
CERT-000001

Blockchain
Base

Status
Verified
```

------------------------------------------------------------------------

# 12. Blockchain 구조

중요한 원칙:

> GitHub 원본 데이터를 블록체인에 저장하지 않는다.

대신 DB에서 관리하는 Certificate의 핵심 정보를 정규화하고 Hash를 생성한
후 Blockchain에 기록한다.

``` text
GitHub Data
     │
     ▼
Contribution Analysis
     │
     ▼
Certificate Metadata
     │
     ▼
Canonical JSON
     │
     ▼
SHA-256 / keccak256 Hash
     │
     ▼
Smart Contract
     │
     ▼
Blockchain
```

------------------------------------------------------------------------

# 13. Smart Contract 역할

Smart Contract는 Certificate의 발급 및 검증 정보를 관리한다.

예상 기능:

-   Certificate 발급
-   Certificate 조회
-   Certificate 폐기
-   Certificate 상태 확인
-   발급자 확인
-   Contributor Wallet 확인
-   Certificate Hash 저장

예상 데이터:

``` text
certificateId
contributor
issuer
projectHash
certificateHash
issuedAt
revoked
```

------------------------------------------------------------------------

# 14. Certificate 검증

공개 URL을 통해 누구나 Certificate를 검증할 수 있도록 한다.

예:

``` text
/verify/CERT-000001
```

검증 페이지:

``` text
Certificate #000001

Project
Pogun

Contributor
username

Issued
2026-08-14

Blockchain
Base

On-chain Status
✓ Valid

Certificate Hash
0x1234...abcd

[View Transaction]
```

검증 과정:

``` text
Certificate ID
      ↓
DB Metadata 조회
      ↓
Hash 재계산
      ↓
Blockchain Hash 조회
      ↓
Hash 비교
      ↓
Valid / Invalid
```

------------------------------------------------------------------------

# 15. 데이터 모델 초안

## User

``` text
User
- id
- githubId
- githubUsername
- email
- createdAt
- updatedAt
```

## GitHubAccount

``` text
GitHubAccount
- id
- userId
- githubId
- accessToken
- tokenExpiresAt
```

실제 구현에서는 Token 암호화 및 보안 저장을 고려한다.

## Repository

``` text
Repository
- id
- githubRepositoryId
- owner
- name
- url
- language
- createdAt
```

## Contribution

``` text
Contribution
- id
- repositoryId
- userId
- commitCount
- pullRequestCount
- reviewCount
- issueCount
- additions
- deletions
- periodStart
- periodEnd
```

## ContributionAnalysis

``` text
ContributionAnalysis
- id
- contributionId
- score
- summary
- technicalAreas
- aiAnalysis
- createdAt
```

## Certificate

``` text
Certificate
- id
- certificateId
- userId
- repositoryId
- analysisId
- certificateHash
- walletAddress
- blockchainNetwork
- transactionHash
- status
- issuedAt
- revokedAt
```

------------------------------------------------------------------------

# 16. API 설계 초안

## Authentication

``` text
GET  /api/auth/github
GET  /api/auth/github/callback
POST /api/auth/logout
GET  /api/auth/me
```

## GitHub

``` text
GET /api/github/profile
GET /api/github/repositories
GET /api/github/repositories/{repositoryId}
```

## Analysis

``` text
POST /api/analyses
GET  /api/analyses/{analysisId}
GET  /api/repositories/{repositoryId}/analysis
```

## Certificate

``` text
POST /api/certificates
GET  /api/certificates
GET  /api/certificates/{certificateId}
POST /api/certificates/{certificateId}/revoke
```

## Verification

``` text
GET /api/verify/{certificateId}
```

Blockchain 트랜잭션 자체는 사용자의 Wallet 서명이 필요한 경우
프론트엔드에서 처리하고, 백엔드는 필요한 메타데이터와 검증 정보를
관리한다.

------------------------------------------------------------------------

# 17. Frontend 페이지 구조

``` text
/
├── Landing
│
├── login
│   └── GitHub Login
│
├── dashboard
│   ├── Overview
│   ├── Repositories
│   ├── Contributions
│   └── Certificates
│
├── repositories
│   └── [repositoryId]
│       ├── Overview
│       ├── Contributions
│       └── Analysis
│
├── certificates
│   └── [certificateId]
│
├── profile
│   └── [username]
│
└── verify
    └── [certificateId]
```

------------------------------------------------------------------------

# 18. Backend 패키지 구조 초안

``` text
com.example.project
├── auth
│   ├── controller
│   ├── service
│   └── domain
│
├── github
│   ├── controller
│   ├── service
│   ├── client
│   └── dto
│
├── contribution
│   ├── controller
│   ├── service
│   ├── domain
│   └── repository
│
├── analysis
│   ├── controller
│   ├── service
│   ├── ai
│   └── domain
│
├── certificate
│   ├── controller
│   ├── service
│   ├── domain
│   └── repository
│
├── blockchain
│   ├── service
│   ├── client
│   └── dto
│
└── common
    ├── security
    ├── exception
    └── config
```

------------------------------------------------------------------------

# 19. 개발 단계

## Phase 0 --- 프로젝트 초기화

-   [ ] GitHub Repository 생성
-   [ ] Next.js 프로젝트 생성
-   [ ] Spring Boot 프로젝트 생성
-   [ ] PostgreSQL 구성
-   [ ] Redis 구성
-   [ ] Docker Compose 구성
-   [ ] 기본 CI 구성
-   [ ] 환경 변수 구조 정의

------------------------------------------------------------------------

## Phase 1 --- 인증

-   [ ] GitHub OAuth App 생성
-   [ ] OAuth 로그인 구현
-   [ ] 사용자 저장
-   [ ] 로그인 세션/JWT 구조 결정
-   [ ] 로그아웃 구현
-   [ ] GitHub Token 보안 저장

완료 기준:

> GitHub 계정으로 로그인하고 자신의 사용자 정보를 확인할 수 있다.

------------------------------------------------------------------------

## Phase 2 --- GitHub Integration

-   [ ] Repository 목록 조회
-   [ ] Repository 상세 조회
-   [ ] Commit 조회
-   [ ] Pull Request 조회
-   [ ] Issue 조회
-   [ ] Review 조회
-   [ ] API 응답 캐싱
-   [ ] Rate Limit 처리

완료 기준:

> 특정 Repository의 사용자 활동 데이터를 안정적으로 가져올 수 있다.

------------------------------------------------------------------------

## Phase 3 --- Contribution Analysis

-   [ ] Contribution 데이터 모델링
-   [ ] 수집 데이터 저장
-   [ ] Contribution Score 계산
-   [ ] 활동 기간 계산
-   [ ] 기술 영역 분류
-   [ ] 분석 결과 저장
-   [ ] Dashboard 구현

완료 기준:

> Repository 하나를 선택하면 사용자의 Contribution Summary를 확인할 수
> 있다.

------------------------------------------------------------------------

## Phase 4 --- AI Analysis

-   [ ] OpenAI API 연동
-   [ ] 분석용 Prompt 설계
-   [ ] GitHub 데이터 → AI 입력 변환
-   [ ] Summary 생성
-   [ ] Technical Area 생성
-   [ ] 결과 저장
-   [ ] AI 결과 재생성 기능 고려

완료 기준:

> Contribution 데이터를 기반으로 사람이 읽을 수 있는 분석 결과를 생성할
> 수 있다.

------------------------------------------------------------------------

## Phase 5 --- Certificate

-   [ ] Certificate 데이터 모델 생성
-   [ ] Certificate Metadata 생성
-   [ ] Canonical JSON 규칙 정의
-   [ ] Certificate Hash 생성
-   [ ] Certificate UI 구현
-   [ ] Certificate ID 생성
-   [ ] Public Certificate 페이지 구현

완료 기준:

> 사용자가 자신의 Contribution Analysis 결과를 Certificate 형태로 확인할
> 수 있다.

------------------------------------------------------------------------

## Phase 6 --- Smart Contract

-   [ ] Foundry 프로젝트 생성
-   [ ] Solidity Contract 작성
-   [ ] Certificate 구조 정의
-   [ ] Issue 함수 구현
-   [ ] Revoke 함수 구현
-   [ ] Verify 함수 구현
-   [ ] Unit Test
-   [ ] Base Sepolia 배포

완료 기준:

> 테스트넷에서 Certificate 정보를 발급하고 조회할 수 있다.

------------------------------------------------------------------------

## Phase 7 --- Wallet Integration

-   [ ] Wallet 연결 UI
-   [ ] Wallet Address 확인
-   [ ] Contract ABI 연결
-   [ ] viem 연동
-   [ ] Transaction 요청
-   [ ] Transaction 상태 처리
-   [ ] Transaction Hash 저장

완료 기준:

> 사용자가 자신의 Wallet으로 Certificate 발급 트랜잭션에 서명할 수 있다.

------------------------------------------------------------------------

## Phase 8 --- Public Verification

-   [ ] Certificate ID 조회
-   [ ] DB Hash 계산
-   [ ] Blockchain Hash 조회
-   [ ] Hash 비교
-   [ ] Valid / Invalid 상태 표시
-   [ ] Transaction 링크
-   [ ] 공유 가능한 검증 URL

완료 기준:

> 제3자가 로그인하지 않고 Certificate의 유효성을 확인할 수 있다.

------------------------------------------------------------------------

## Phase 9 --- 배포

-   [ ] Frontend 배포
-   [ ] Backend 배포
-   [ ] PostgreSQL 배포
-   [ ] Redis 배포
-   [ ] Domain 연결
-   [ ] HTTPS
-   [ ] GitHub OAuth Production 설정
-   [ ] Smart Contract Production 환경 설정
-   [ ] Monitoring / Logging

------------------------------------------------------------------------

# 20. MVP 범위

첫 번째 완성 버전에서는 기능을 욕심내지 않는다.

### 반드시 포함

-   GitHub 로그인
-   Repository 선택
-   Commit / PR / Issue / Review 수집
-   Contribution Score
-   기본 AI Summary
-   Certificate 생성
-   Wallet 연결
-   Base Sepolia Certificate 발급
-   공개 검증 페이지

### MVP에서 제외

-   로컬 Git 분석
-   데스크톱 앱
-   다중 블록체인 지원
-   NFT Marketplace
-   토큰 발행
-   복잡한 DAO
-   조직용 관리자 시스템
-   실시간 GitHub Webhook
-   고급 AI 평가

------------------------------------------------------------------------

# 21. 보안 고려사항

## GitHub Token

-   평문 저장 금지
-   암호화 저장
-   필요 최소 권한 요청
-   로그 출력 금지

## Wallet

-   Private Key를 서버에서 저장하지 않는다.
-   트랜잭션 서명은 사용자 Wallet에서 수행한다.

## Blockchain

블록체인에는 민감한 개인정보나 GitHub 원본 데이터를 기록하지 않는다.

온체인에는 Certificate 검증에 필요한 최소한의 데이터만 저장한다.

------------------------------------------------------------------------

# 21. 블록체인을 사용하는 이유

이 프로젝트에서 블록체인은 데이터를 저장하기 위한 DB 대체재가 아니다.

역할은 다음과 같다.

``` text
PostgreSQL
= 서비스 데이터 저장

Blockchain
= Certificate 검증 정보의 독립적인 증명
```

즉 서비스가 나중에 사라지거나 DB가 변경되더라도 온체인에 기록된
Certificate의 발급 및 Hash 정보를 제3자가 확인할 수 있도록 하는 것이
핵심이다.

------------------------------------------------------------------------

# 21. 프로젝트 차별점

단순한 GitHub 통계 서비스:

> "Commit 438개 했습니다."

에서 끝나지 않는다.

이 프로젝트는:

``` text
GitHub Activity
      ↓
Contribution Analysis
      ↓
AI Summary
      ↓
Certificate
      ↓
Blockchain Proof
      ↓
Public Verification
```

까지 연결한다.

따라서 최종적으로는 **개발자의 GitHub 활동을 검증 가능한 디지털 경력
증명으로 바꾸는 플랫폼**을 목표로 한다.

------------------------------------------------------------------------

# 21. 최종 기술 스택 요약

``` text
Frontend
- Next.js
- TypeScript
- Tailwind CSS
- shadcn/ui

Backend
- Spring Boot
- Java 또는 Kotlin

Database
- PostgreSQL

Cache / Async
- Redis

External API
- GitHub OAuth
- GitHub REST / GraphQL API
- OpenAI API

Blockchain
- Solidity
- Foundry
- Base Sepolia
- Base Mainnet
- viem

Wallet
- MetaMask
- WalletConnect

Infrastructure
- Docker
- GitHub Actions
- Vercel
- AWS
```

------------------------------------------------------------------------

# 21. 개발 원칙

1.  처음부터 모든 기능을 만들지 않는다.
2.  GitHub 기반 MVP를 먼저 완성한다.
3.  블록체인은 DB가 아니라 검증 레이어로 사용한다.
4.  Wallet Private Key를 서버에 저장하지 않는다.
5.  AI 분석과 실제 GitHub 데이터를 명확히 구분한다.
6.  Contribution Score를 개발자의 절대적인 실력으로 표현하지 않는다.
7.  공개 검증 기능을 프로젝트의 핵심 기능으로 유지한다.
8.  Tauri와 Local Git 분석은 V2로 미룬다.
9.  기능보다 서비스 흐름의 완성도를 우선한다.
10. 테스트넷에서 충분히 검증한 뒤 Mainnet으로 확장한다.

------------------------------------------------------------------------

# 21. 최종 목표

최종적으로 사용자가 GitHub 계정으로 로그인해서 자신의 프로젝트를
선택하고,

> "나는 이 프로젝트에서 이런 방식으로 기여했고, 이 분석 결과는
> 블록체인에서 검증할 수 있다."

라고 다른 사람에게 보여줄 수 있는 서비스를 만든다.

핵심은 **GitHub + AI + Blockchain을 억지로 묶는 것이 아니라,
Contribution을 검증 가능한 경력 데이터로 만드는 것**이다.
