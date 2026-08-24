# Contribution Attestation Platform

GitHub 기여 데이터를 재현 가능한 Snapshot과 Score로 분석하고, Certificate를 Base Sepolia에 self-attest한 뒤 공개 검증하는 monorepo야.

- `Backend`: Spring Boot API, OAuth, 분석 작업, Certificate/Blockchain 검증
- `Frontend`: Next.js Dashboard, Wallet 발급·폐기, 공개 검증 화면
- `contracts`: Foundry 기반 ContributionAttestation contract

로컬 구성은 [monorepo guide](docs/monorepo.md), 운영 배포와 OAuth·백업·모니터링 절차는 [production operations](docs/operations.md)를 참고해.
