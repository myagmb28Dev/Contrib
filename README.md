# Contribution Attestation Platform

GitHub 기여 데이터를 재현 가능한 Snapshot과 Score로 분석하고, Certificate를 Base Sepolia에 self-attest한 뒤 공개 검증하는 monorepo입니다.

- `Backend`: Spring Boot API, OAuth, 분석 작업, Certificate/Blockchain 검증
- `Frontend`: Next.js Dashboard, Wallet 발급·폐기, 공개 검증 화면
- `contracts`: Foundry 기반 ContributionAttestation contract

증명서는 서버에 생성한 뒤 온체인 등록 없이도 조회하고 공유할 수 있습니다.
온체인 등록은 선택 사항입니다.
증명서의 `ISSUED`는 생성 완료를 뜻하며, 공개 검증 상태는 `NOT_REGISTERED`(온체인 미등록),
`PENDING`(온체인 등록 처리 중), `VALID`(온체인 검증 완료)로 구분할 수 있습니다.
