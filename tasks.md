# Video Station - 구현 계획

> [prd-video.md](./prd-video.md) | [spec.md](./spec.md)

---

## Phase 1: 프로젝트 기반 구축

### 1.1 백엔드 프로젝트 초기화
- [x] Spring Boot 4.0.5 + Java 25 + Gradle Kotlin DSL 프로젝트 생성
- [x] 의존성 추가 (Spring Web, JPA, Security, Validation, Lombok, MySQL Driver, JWT, S3 SDK)
- [x] application.yml 환경 설정 (DB, 파일 경로, JWT 시크릿 등)
- [x] 패키지 구조 생성 (api, application, domain, persistence, global, encoding, event)

### 1.2 프론트엔드 프로젝트 초기화
- [x] Next.js (App Router) + TypeScript + Tailwind CSS 프로젝트 생성
- [x] 디렉토리 구조 생성 (app, components, hooks, lib, types)
- [x] API 클라이언트 설정 (fetch wrapper, 인터셉터, 토큰 관리)

### 1.3 인프라 설정
- [x] Docker Compose 작성 (MySQL 8)
- [x] Nginx 설정 파일 작성 (HLS 서빙 + 리버스 프록시 + API 프록시)
- [x] 로컬 개발용 디렉토리 구조 생성 스크립트 (init-dirs.sh)

### 1.4 JPA 엔티티
- [x] BaseEntity (createdAt, updatedAt) 공통 엔티티
- [x] User 엔티티 + UserRole enum (SUPER_ADMIN, ADMIN, VIEWER) + UserStatus enum (ACTIVE, INACTIVE, BANNED)
- [x] Video 엔티티 + VideoStatus enum (UPLOADING, ENCODING_QUEUED, ENCODING, READY, FAILED, DELETED)
- [x] Playlist 엔티티
- [x] PlaylistVideo 엔티티 (sortOrder 포함)
- [x] Repository 인터페이스 (UserRepository, VideoRepository, PlaylistRepository, PlaylistVideoRepository)

### 1.5 JWT 인증 시스템
- [x] JWT 토큰 생성/검증 유틸 (Access Token 30분, Refresh Token 7일) ✅ 5 tests
- [x] UserPrincipal (Spring Security UserDetails 구현)
- [x] TokenAuthenticationFilter (요청 헤더에서 JWT 추출 → 인증)
- [x] SecurityConfig (경로별 권한 설정, CORS, CSRF 비활성화)
- [x] Refresh Token HttpOnly 쿠키 처리

### 1.6 인증 API (`/api/v1/auth`) ✅ 8 tests
- [x] POST `/register` - 회원가입 (기본 VIEWER)
- [x] POST `/login` - 로그인 (Access Token + Refresh Token 발급)
- [x] POST `/refresh` - 토큰 갱신
- [x] GET `/me` - 현재 사용자 정보

### 1.7 공통 모듈
- [x] GlobalExceptionHandler (에러 응답 통일)
- [x] API 응답 DTO 공통 포맷 (ErrorResponse, ErrorCode, BusinessException)
- [x] OpenAPI 스펙 (openapi.yml)

---

## Phase 2: 동영상 관리

### 2.1 동영상 업로드
- [x] 동영상 업로드 API - POST `/api/v1/admin/videos` (multipart)
- [x] 원본 파일 로컬 디스크 저장 (FileStorageService)
- [x] Video 엔티티 생성 (status: UPLOADING → ENCODING_QUEUED)
- [x] 파일 크기, 원본 파일명 메타데이터 저장

### 2.2 FFmpeg 인코딩 파이프라인
- [x] FFmpegEncoder - FFmpeg 프로세스 실행 (1080p HLS 인코딩) ✅ 3 tests
- [x] 썸네일 자동 추출 (영상 30초 지점)
- [x] 영상 길이(durationSeconds) 추출
- [x] EncodingQueue - 동시 인코딩 1~2개 제한 (Semaphore), @Async
- [x] EncodingEventListener - 인코딩 완료/실패 이벤트 처리 ✅ 3 tests
- [x] 비동기 실행 (@Async)

### 2.3 Object Storage 백업
- [x] NCP Object Storage 클라이언트 설정 (S3 호환 API)
- [x] 업로드 완료 후 비동기로 원본 백업
- [x] objectStorageKey 필드 갱신

### 2.4 동영상 CRUD API (`/api/v1/admin/videos`) ✅ 8 tests
- [x] GET `/` - 관리자 영상 목록 (페이징, 상태 필터, 검색)
- [x] GET `/{videoId}` - 상세 조회
- [x] PUT `/{videoId}` - 메타데이터 수정 (제목, 설명, 태그)
- [x] DELETE `/{videoId}` - 삭제 (soft delete, status → DELETED)
- [x] PATCH `/{videoId}/visibility` - 공개/비공개 전환
- [x] GET `/{videoId}/encoding-status` - 인코딩 진행 상태

### 2.5 프론트엔드: 동영상 관리
- [x] 어드민 레이아웃 (사이드바 + 헤더)
- [x] 동영상 업로드 페이지 (드래그앤드롭)
- [x] 동영상 목록 페이지 (테이블, 상태 배지, 페이징)
- [x] 동영상 상세/수정 페이지
- [x] 인코딩 상태 표시 (폴링으로 상태 갱신)

---

## Phase 3: 재생목록

### 3.1 재생목록 CRUD API (`/api/v1/admin/playlists`) ✅ 8 tests
- [x] POST `/` - 재생목록 생성
- [x] GET `/` - 재생목록 목록
- [x] GET `/{playlistId}` - 상세 (영상 목록 + 순서 포함)
- [x] PUT `/{playlistId}` - 수정 (이름, 설명, 공개 여부)
- [x] DELETE `/{playlistId}` - 삭제

### 3.2 재생목록 영상 관리 API ✅ PlaylistService 10 tests
- [x] POST `/{playlistId}/videos` - 영상 추가
- [x] DELETE `/{playlistId}/videos/{videoId}` - 영상 제거
- [x] PUT `/{playlistId}/videos/reorder` - 순서 변경

### 3.3 프론트엔드: 재생목록 관리
- [x] 재생목록 목록 페이지
- [x] 재생목록 생성/수정 폼
- [x] 재생목록 에디터 (영상 추가/제거, 순서 변경 ▲▼)

---

## Phase 4: 시청자 페이지

### 4.1 시청자 API
- [ ] GET `/api/v1/videos` - 공개 영상 목록 (페이징, 검색)
- [ ] GET `/api/v1/videos/{videoId}` - 영상 재생 정보 (HLS URL + 메타데이터)
- [ ] GET `/api/v1/playlists` - 공개 재생목록 목록
- [ ] GET `/api/v1/playlists/{playlistId}` - 재생목록 상세 (영상 순서 포함)
- [ ] 조회수 카운트 로직 (영상 재생 시 viewCount 증가)

### 4.2 HLS 비디오 플레이어
- [ ] useHlsPlayer 훅 (HLS.js 초기화, 소스 로딩, 에러 처리)
- [ ] VideoPlayer 컴포넌트 (재생 컨트롤, 프로그레스바, 볼륨)
- [ ] HLS 미지원 브라우저 폴백 (네이티브 video 태그)

### 4.3 시청자 페이지
- [ ] 영상 목록 페이지 (썸네일 그리드, 검색, 페이징)
- [ ] 영상 상세 재생 페이지 (플레이어 + 영상 정보)
- [ ] 재생목록 목록 페이지
- [ ] 재생목록 재생 페이지 (자동 다음 영상, 영상 목록 사이드바, 이전/다음 버튼)

### 4.4 인증 프론트엔드
- [ ] 로그인 페이지
- [ ] 회원가입 페이지
- [ ] useAuth 훅 (로그인 상태 관리, 토큰 갱신, 로그아웃)
- [ ] 인증 가드 (미로그인 시 로그인 페이지로 리다이렉트)

---

## Phase 5: 관리 및 마무리

### 5.1 사용자 관리 (`/api/v1/admin/users`) - SUPER_ADMIN
- [ ] GET `/` - 사용자 목록 (페이징)
- [ ] PATCH `/{userId}/role` - 역할 변경
- [ ] PATCH `/{userId}/status` - 상태 변경 (정지/활성)
- [ ] DELETE `/{userId}` - 사용자 삭제
- [ ] 프론트엔드: 사용자 관리 페이지 (목록, 역할/상태 변경)

### 5.2 에러 핸들링 및 로깅
- [ ] 백엔드 로깅 설정 (Logback)
- [ ] 프론트엔드 에러 바운더리
- [ ] API 에러 응답 일관성 검토

### 5.3 배포 준비
- [ ] Backend Dockerfile 작성
- [ ] Frontend Dockerfile 작성
- [ ] docker-compose.yml 통합 (MySQL + Backend + Frontend + Nginx)
- [ ] 환경변수 분리 (.env)
