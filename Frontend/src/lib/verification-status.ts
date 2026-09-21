export function verificationStatusLabel(status: string): string {
  switch (status) {
    case "NOT_REGISTERED": return "온체인 미등록";
    case "PENDING": return "온체인 등록 처리 중";
    case "VALID": return "온체인 검증 완료";
    case "REVOKED": return "증명서 폐기됨";
    case "HASH_MISMATCH": return "증명 정보 불일치";
    case "CHAIN_UNAVAILABLE": return "온체인 확인 불가";
    case "NOT_FOUND": return "증명서를 찾을 수 없음";
    default: return "상태 확인 불가";
  }
}
