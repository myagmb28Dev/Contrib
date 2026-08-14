# Frontend

Next.js App Router 기반 웹 영역이야.

계획서의 화면 경계를 `src/app` 라우팅으로 유지해.

- `/`: Landing
- `/login`: GitHub OAuth 진입
- `/dashboard/*`: 로그인 사용자 화면
- `/repositories/[repositoryId]/*`: Repository와 Analysis
- `/certificates/[certificateId]`: 개인 Certificate
- `/verify/[publicId]`: 로그인 없는 공개 검증

## 실행

```powershell
npm install
npm run dev
```

Backend API 주소는 `NEXT_PUBLIC_API_BASE_URL`로 바꿀 수 있어.

