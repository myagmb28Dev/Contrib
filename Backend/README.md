# Backend

Spring Boot REST API 영역이야.

## 패키지 경계

`com.example.project` 아래에 계획서의 기능 경계를 그대로 둬.

- `auth`: GitHub OAuth와 사용자 계정
- `github`: GitHub API Client, 외부 DTO, Mapper
- `repository`: Repository 조회와 저장
- `analysis`: 비동기 Job, 수집, 지표, Score
- `certificate`: Payload, Canonical JSON, Hash, 발급 상태
- `blockchain`: Wallet 트랜잭션과 Receipt 연동
- `verification`: 로그인 없는 공개 검증
- `common`: 설정, 보안, 예외, 응답, 감사 로그

외부 GitHub DTO가 내부 Domain으로 새어 들어오지 않도록 `github/mapper`를 경계로 사용해.

## 실행

```powershell
.\gradlew.bat bootRun
```
