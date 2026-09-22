# Azure deployment

Contrib uses Azure for Students with its spending limit enabled. Do not convert the subscription to pay-as-you-go. The student credit is a finite budget, not a guarantee of free operation through December.

## Architecture

- Azure Container Apps Consumption environment in Korea Central.
- Backend: 0.25 vCPU / 0.5 GiB, minimum and maximum one replica. Keep it running for the database-backed analysis dispatcher and blockchain receipt polling; external keep-alive pings are unnecessary.
- Frontend: 0.25 vCPU / 0.5 GiB, zero to one replica. It can sleep between requests and wake on demand.
- Backend ingress is internal. The frontend proxies `/api/*` and `/oauth2/*`, so browser requests and session cookies use the same public origin.
- Existing Neon PostgreSQL is retained. Never regenerate the token encryption key: it protects existing GitHub credentials in the database.
- ACR Basic stores private images. Apps use a pull-only managed identity; GitHub Actions uses a separate OIDC identity restricted to this repository's `main` branch.
- Deployment identity has AcrPush only on this registry, plus Container Apps Contributor only on the two application resources. No subscription-wide Contributor role or stored Azure API secret is needed.
- No dedicated workload profile or Log Analytics workspace is provisioned. Live logs remain available; historical log retention requires a separately budgeted configuration.

## Configuration

Frontend build arguments:

```text
NEXT_PUBLIC_API_BASE_URL=/
API_PROXY_TARGET=https://<backend internal FQDN>
```

Backend production settings include `FRONTEND_URL` and `CORS_ALLOWED_ORIGINS` set to the frontend HTTPS origin, `GITHUB_REDIRECT_URI=<frontend origin>/api/auth/github/callback/github`, `SESSION_COOKIE_SECURE=true`, and `SESSION_COOKIE_SAME_SITE=lax`.

The GitHub OAuth App's homepage and callback must match this public origin. The callback must never point at the internal backend hostname. Store database credentials, token encryption key, OAuth secret, monitoring token, and AI API key as Container Apps secrets; do not put them in Git or GitHub Actions logs.

Use HTTP readiness and liveness probes on `/actuator/health/readiness` and `/actuator/health/liveness` for the backend, and `/` for the frontend. A startup probe should allow several minutes for Spring Boot on the small CPU allocation.

## Releasing

1. Merge a reviewed feature PR and wait for the `CI` workflow to pass on that exact `main` commit.
2. Set repository variables `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, `AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, `AZURE_REGISTRY`, `AZURE_API_PROXY_TARGET`, and `AZURE_PUBLIC_ORIGIN`.
3. Run **Deploy Azure** on `main`. The workflow verifies the CI result, builds both images with the commit SHA, and updates the apps using OIDC. It does not run automatically on every push, to avoid unwanted student-credit consumption.
4. For first-time provisioning only, run with `deploy_apps=false` to publish the images before creating the apps with the pull identity and secret configuration. Subsequent runs use `deploy_apps=true`.
5. Verify the homepage, CSRF endpoint, actual GitHub sign-in, existing repository/analysis/certificate data, and a completed analysis job. A successful image build alone is not a completed deployment.

## Cost and lifecycle

The backend has a continuing compute cost. Container Apps monthly free grants are shared by the apps; ACR, network egress, and any additional resources have separate costs. Keep max replicas at one and review remaining student credit regularly. Frequent keep-alive requests to the frontend prevent scale-to-zero and waste credit. Health monitoring should detect failures, not defeat scaling.

Plan retirement after the December 2026 project period: export any required data, remove application resources, and retain or delete the database only by explicit decision. No automatic deletion has been scheduled.
