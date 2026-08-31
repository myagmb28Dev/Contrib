# Contrib Render 배포 가이드

본 문서는 **Contrib (Next.js 프론트엔드 + Spring Boot 백엔드 + Neon Cloud PostgreSQL)** 서비스를 클라우드 플랫폼 [Render](https://render.com)에 안정적으로 배포하고 `.env` 환경변수를 연동하는 전체 과정을 설명합니다.

---

## 1. 사전 준비 사항

1. [Render](https://render.com) 계정 가입 및 로그인
2. Contrib 저장소를 GitHub에 푸시 (작업 브랜치 `feature/render-deployment` 또는 `main`)
3. GitHub OAuth App 생성 또는 Callback URL 갱신 ([GitHub Developer Settings](https://github.com/settings/developers))
   - **Homepage URL**: `https://contrib-frontend.onrender.com`
   - **Authorization callback URL**: `https://contrib-backend.onrender.com/api/auth/github/callback/github`
4. Neon Serverless PostgreSQL 연결 정보 (`.env`에 기재된 `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`)

---

## 2. 배포 방식 1: Render Blueprint (원클릭 자동 배포, 권장)

저장소 루트에 포함된 `render.yaml` 파일을 사용하여 백엔드와 프론트엔드를 한 번에 배포하는 방식입니다.

### 배포 절차

1. **Render 대시보드 접속**: [dashboard.render.com](https://dashboard.render.com)
2. **New 버튼 클릭 -> [Blueprints] 선택**
3. **Contrib GitHub 저장소 연결**
4. **Blueprint 설정 확인**:
   - `contrib-backend` (Spring Boot Web Service)
   - `contrib-frontend` (Next.js Web Service)
5. **환경변수 확인 및 [Apply] 버튼 클릭**:
   - `render.yaml`에 정의된 2개의 웹 서비스가 순차적으로 빌드 및 배포됩니다.
   - 빌드가 완료되면 아래 주소로 서비스가 활성화됩니다:
     - 백엔드: `https://contrib-backend.onrender.com`
     - 프론트엔드: `https://contrib-frontend.onrender.com`

---

## 3. 배포 방식 2: Render 대시보드 수동 배포

각 서비스를 개별적으로 생성하여 연결하는 방식입니다.

### Step 1: 백엔드 Web Service 생성 (`contrib-backend`)

1. Render 대시보드 -> **New -> Web Service**
2. Contrib 저장소 선택
3. 기본 설정:
   - **Name**: `contrib-backend`
   - **Region**: `Singapore (Southeast Asia)` (또는 Neon DB와 동일한 리전)
   - **Branch**: `feature/render-deployment` 또는 `main`
   - **Runtime**: `Docker`
   - **Docker Context**: `./Backend`
   - **Dockerfile Path**: `./Backend/Dockerfile`
   - **Instance Type**: `Free`
4. **Environment Variables (환경변수)** 등록 (`.env` 파일의 값을 참조하여 입력):
   - `SPRING_PROFILES_ACTIVE`: `prod`
   - `DATABASE_URL`: `.env`의 `DATABASE_URL` (Neon Cloud PostgreSQL JDBC URL)
   - `POSTGRES_USER`: `.env`의 `POSTGRES_USER`
   - `POSTGRES_PASSWORD`: `.env`의 `POSTGRES_PASSWORD`
   - `TOKEN_ENCRYPTION_KEY`: `.env`의 `TOKEN_ENCRYPTION_KEY`
   - `GITHUB_CLIENT_ID`: `.env`의 `GITHUB_CLIENT_ID`
   - `GITHUB_CLIENT_SECRET`: `.env`의 `GITHUB_CLIENT_SECRET`
   - `FRONTEND_URL`: `https://contrib-frontend.onrender.com`
   - `CORS_ALLOWED_ORIGINS`: `https://contrib-frontend.onrender.com,http://localhost:3000`
   - `SESSION_COOKIE_SECURE`: `true`
   - `SESSION_COOKIE_SAME_SITE`: `none`
   - `MONITORING_TOKEN`: `contrib-prod-monitoring-token-secret-key-32ch` (또는 32자 이상의 임의 비밀 문자열)
   - `BASE_SEPOLIA_RPC_URL`: `https://base-sepolia-rpc.publicnode.com`
   - `ATTESTATION_CONTRACT_ADDRESS`: `.env`의 `ATTESTATION_CONTRACT_ADDRESS`
   - `AI_PROVIDER`: `openrouter`
   - `OPENROUTER_API_KEY`: `.env`의 `OPENROUTER_API_KEY`
   - `OPENROUTER_MODEL`: `google/gemini-2.0-flash-001`
   - `OPENROUTER_SITE_URL`: `https://contrib-frontend.onrender.com`
   - `OPENROUTER_APP_NAME`: `Contrib`
5. **Create Web Service** 클릭하여 빌드 시작

---

### Step 2: 프론트엔드 Web Service 생성 (`contrib-frontend`)

1. Render 대시보드 -> **New -> Web Service**
2. Contrib 저장소 선택
3. 기본 설정:
   - **Name**: `contrib-frontend`
   - **Region**: `Singapore (Southeast Asia)`
   - **Branch**: `feature/render-deployment` 또는 `main`
   - **Runtime**: `Docker`
   - **Docker Context**: `./Frontend`
   - **Dockerfile Path**: `./Frontend/Dockerfile`
   - **Instance Type**: `Free`
4. **Docker Build Arguments (빌드 인자)** 추가:
   - **Key**: `NEXT_PUBLIC_API_BASE_URL`
   - **Value**: `https://contrib-backend.onrender.com`
5. **Environment Variables**:
   - `NODE_ENV`: `production`
6. **Create Web Service** 클릭하여 빌드 시작

---

## 4. 라이브 E2E 실측 검증 프로토콜

배포 완료(Live) 후 아래 5단계 실측 검증을 통해 서비스가 정상 작동하는지 확인합니다:

1. **백엔드 인프라 헬스체크**:
   ```bash
   curl -I https://contrib-backend.onrender.com/actuator/health
   # HTTP 200 OK 확인 (status: UP)
   ```
2. **보안/인증 CSRF 토큰 발급**:
   ```bash
   curl -I https://contrib-backend.onrender.com/api/auth/csrf
   # HTTP 200 OK 확인 (XSRF-TOKEN 쿠키 및 응답 바디 확인)
   ```
3. **GitHub OAuth 로그인 인증**:
   - `https://contrib-frontend.onrender.com/login` 접속 -> [GitHub으로 계속하기] 클릭
   - GitHub OAuth 인가 후 `https://contrib-frontend.onrender.com/dashboard`로 정상 리다이렉트 확인
   - `JSESSIONID` 세션 쿠키 발급 및 `SameSite=None; Secure` 속성 적용 확인
4. **Neon PostgreSQL DB 데이터 조회**:
   - 대시보드에서 동기화된 GitHub 저장소 목록(`/api/repositories`), 기여도 분석 내역(`/api/analyses`), 인증서(`/api/certificates`)가 정상 조회되는지 확인
5. **프론트엔드 라이브 UI 렌더링**:
   - 메인 랜딩 페이지, 로그인 페이지, 대시보드 페이지가 스타일 깨짐 없이 정상 렌더링되는지 확인

---

## 5. 트러블슈팅 가이드

- **Free Tier 초기 슬립 모드(Cold Start)**:
  - Render 무료 인스턴스는 15분간 요청이 없으면 슬립 모드로 전환됩니다. 첫 접속 시 30~50초 정도 로딩이 걸릴 수 있으며 이후 정상 응답합니다.
- **GitHub OAuth 리다이렉트 오류 (redirect_uri_mismatch)**:
  - GitHub Developer Settings의 OAuth App에서 Authorization callback URL이 정확히 `https://contrib-backend.onrender.com/api/auth/github/callback/github`로 설정되어 있는지 확인합니다.
- **CORS 오류**:
  - 백엔드 환경변수 `CORS_ALLOWED_ORIGINS`에 프론트엔드 도메인(`https://contrib-frontend.onrender.com`)이 정확히 포함되어 있는지 확인합니다.
- **세션 쿠키가 프론트엔드로 전달되지 않는 현상**:
  - 프론트엔드와 백엔드가 서로 다른 Render 서브도메인을 사용할 때는 브라우저의 서드파티 쿠키 정책으로 인해 `SESSION_COOKIE_SECURE=true` 및 `SESSION_COOKIE_SAME_SITE=none` 설정이 필수적입니다.
