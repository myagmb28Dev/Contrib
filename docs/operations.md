# Production operations

실제 AI Provider는 아직 연결하지 않고 `AI_PROVIDER=rule-based`로 운영한다.

## 1. 배포 경계

- Frontend: Vercel 또는 `Frontend/Dockerfile`
- Backend: `Backend/Dockerfile`을 지원하는 컨테이너 플랫폼
- Database: 관리형 PostgreSQL(Neon PostgreSQL과 migration 검증 완료)
- Contract: `contracts/deployments/base-sepolia.json`의 Base Sepolia 주소

Frontend와 Backend는 `app.example.com`, `api.example.com`처럼 같은 상위 도메인의 HTTPS 주소를 권장한다. 서로 다른 사이트를 쓰면 브라우저의 third-party cookie 정책 때문에 OAuth 세션이 차단될 수 있다.

## 2. Secret과 운영 환경

`.env.example`을 `.env.production`으로 복사하되 실제 값은 GitHub Actions나 배포 플랫폼 Secret에만 저장한다. 저장소에는 절대 커밋하지 않는다.

필수 값:

- `DATABASE_URL`, `POSTGRES_USER`, `POSTGRES_PASSWORD`
- `GITHUB_CLIENT_ID`, `GITHUB_CLIENT_SECRET`
- `TOKEN_ENCRYPTION_KEY`: Base64로 인코딩된 정확히 32바이트 키
- `FRONTEND_URL`, `CORS_ALLOWED_ORIGINS`, `NEXT_PUBLIC_API_BASE_URL`: HTTPS 주소
- `ATTESTATION_CONTRACT_ADDRESS`, `BASE_SEPOLIA_RPC_URL`
- `MONITORING_TOKEN`: Prometheus endpoint 전용 32자 이상의 임의 토큰
- `AI_PROVIDER=rule-based`

배포 전 검증:

```powershell
./scripts/validate-production-env.ps1
```

Backend는 `prod` 프로필에서 필수 값, HTTPS Frontend URL, secure cookie, contract address를 시작 시 다시 검증한다.

## 3. OAuth와 HTTPS

GitHub OAuth App의 production 설정은 다음처럼 잡는다.

- Homepage URL: `https://app.example.com`
- Authorization callback URL: `https://api.example.com/api/auth/github/callback/github`

Backend 앞의 load balancer/reverse proxy는 `X-Forwarded-Proto`와 `X-Forwarded-Host`를 전달해야 한다. `prod` 프로필은 forwarded header를 신뢰하고 session/CSRF cookie를 `Secure; SameSite=None`으로 발급한다. Reverse proxy에서는 `/api`, OAuth callback, `/actuator/health/**`만 공개하고 `/actuator/prometheus`는 외부에 노출하지 않는다.

## 4. Container 배포

로컬에서 production artifact를 확인할 때:

```powershell
docker compose --env-file .env.production -f docker-compose.production.yml build
docker compose --env-file .env.production -f docker-compose.production.yml up -d
```

호스팅 플랫폼에서는 각 Dockerfile을 독립 서비스로 배포한다. Frontend build argument `NEXT_PUBLIC_API_BASE_URL`은 Backend의 public HTTPS URL이어야 하며, Backend에는 `SPRING_PROFILES_ACTIVE=prod`를 설정한다.

## 5. Health, metrics, error tracking

- Liveness: `/actuator/health/liveness` 또는 `/livez`
- Readiness: `/actuator/health/readiness` 또는 `/readyz`
- Metrics: `/actuator/prometheus`, header `X-Monitoring-Token: <MONITORING_TOKEN>` 필수
- Alert rules: `ops/prometheus-alerts.yml`

Production 로그는 ECS JSON 형식이다. 모든 HTTP 응답에 `X-Request-ID`가 붙고, 예상하지 못한 500 응답에도 같은 `requestId`가 포함된다. 로그 수집기에서 `requestId`, `errorCode`, `requestPath`로 에러를 추적하고 5xx 비율/지연시간/DB pool 알림을 연결한다.

## 6. PostgreSQL backup

`POSTGRES_BACKUP_URL`에 PostgreSQL URI를 넣고 PostgreSQL client 도구가 설치된 안전한 운영 호스트에서 실행한다.

```powershell
./scripts/backup-postgres.ps1 -Destination D:\Backups\Contrib -RetentionDays 14
```

스크립트는 custom format dump를 만들고 `pg_restore --list`로 즉시 검증한다. 백업은 DB와 다른 저장소에 암호화해 보관하고, 월 1회 별도 DB에 복원 리허설을 수행한다. 관리형 DB의 PITR/보존 기간도 별도로 활성화한다.

## 7. Release smoke test

1. `/actuator/health/readiness`가 `UP`인지 확인한다.
2. GitHub 로그인과 저장소 동기화를 확인한다.
3. 분석 완료 후 규칙 기반 Summary와 Certificate를 생성한다.
4. Wallet으로 Base Sepolia 발급 후 `CONFIRMED`까지 확인한다.
5. 로그인하지 않은 브라우저에서 공개 검증한다.
6. 발급자가 폐기하고 공개 화면이 `REVOKED`인지 확인한다.
7. 로그에서 같은 `X-Request-ID`를 검색하고 metrics scrape/alert routing을 확인한다.
