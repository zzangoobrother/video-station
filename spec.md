# Video Station - 구현 스펙

> 제품 요구사항은 [prd-video.md](./prd-video.md) 참고

---

## 1. 프로젝트 구조

모노레포 구조

```
video-station/
├── backend/                          # Spring Boot 4
│   ├── build.gradle.kts
│   ├── settings.gradle.kts
│   ├── docker/
│   │   └── docker-compose.yml        # MySQL
│   ├── nginx/
│   │   └── nginx.conf                # Nginx 설정 (HLS 서빙 + 리버스 프록시)
│   └── src/main/java/com/videostation/
│       ├── VideoStationApplication.java
│       ├── api/                       # REST Controllers
│       ├── application/               # Service 레이어
│       │   └── dto/                   # request/response DTO
│       ├── domain/                    # JPA Entity
│       │   └── constant/             # Enum (Role, Status 등)
│       ├── persistence/               # Repository
│       │   └── support/
│       │       ├── objectstorage/    # NCP Object Storage (원본 백업용)
│       │       └── ncp/              # NCP API 인증 헬퍼
│       ├── global/                    # Security, Config, Exception
│       │   ├── auth/                 # JWT, UserPrincipal
│       │   ├── config/              # Security, JPA, Swagger
│       │   ├── error/               # GlobalExceptionHandler
│       │   └── filter/              # TokenAuthenticationFilter
│       ├── encoding/                  # FFmpeg 인코딩 처리
│       │   ├── FFmpegEncoder.java
│       │   ├── EncodingQueue.java    # 인코딩 대기열 관리
│       │   └── EncodingEventListener.java
│       └── event/                     # 도메인 이벤트
│
├── frontend/                          # Next.js
│   ├── package.json
│   ├── next.config.ts
│   └── src/
│       ├── app/
│       │   ├── (auth)/               # 로그인, 회원가입
│       │   ├── (viewer)/             # 시청자 페이지
│       │   │   ├── videos/           # 영상 목록 / 상세 재생
│       │   │   └── playlists/        # 재생목록 목록 / 재생
│       │   └── admin/                # 어드민 패널
│       │       ├── videos/           # 동영상 관리
│       │       ├── playlists/        # 재생목록 관리
│       │       └── users/            # 사용자 관리 (최고관리자)
│       ├── components/
│       │   ├── ui/                   # 공통 UI 컴포넌트
│       │   ├── video/                # VideoPlayer (HLS.js)
│       │   ├── playlist/             # PlaylistPlayer, PlaylistEditor 등
│       │   └── layout/               # AdminSidebar, Header 등
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   └── useHlsPlayer.ts       # HLS.js 초기화
│       ├── lib/                       # API 클라이언트, 상수
│       └── types/                     # TypeScript 타입 정의
└── docs/
    ├── prd-video.md
    └── spec.md
```

---

## 2. 데이터베이스 스키마

### 2.1 핵심 엔티티

#### User (사용자)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 사용자 ID |
| email | String (unique) | 이메일 |
| password | String | 비밀번호 (암호화) |
| name | String | 이름 |
| nickname | String | 닉네임 |
| role | Enum | SUPER_ADMIN, ADMIN, VIEWER |
| status | Enum | ACTIVE, INACTIVE, BANNED |
| profileImageUrl | String | 프로필 이미지 URL |
| createdAt / updatedAt | LocalDateTime | 생성/수정 시간 |

#### Video (동영상)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 동영상 ID |
| title | String | 제목 |
| description | Text | 설명 |
| originalFilePath | String | 원본 파일 로컬 경로 |
| originalFileName | String | 원본 파일명 |
| fileSize | Long | 파일 크기 (bytes) |
| thumbnailPath | String | 썸네일 로컬 경로 |
| durationSeconds | Integer | 재생 시간 (초) |
| status | Enum | UPLOADING, ENCODING_QUEUED, ENCODING, READY, FAILED, DELETED |
| hlsPath | String | HLS 마스터 플레이리스트 로컬 경로 |
| objectStorageKey | String | Object Storage 백업 키 (null이면 미백업) |
| tags | String | 태그 (쉼표 구분) |
| isPublic | Boolean | 공개 여부 |
| viewCount | Long | 조회수 |
| uploadedBy | User (FK) | 업로드한 관리자 |
| createdAt / updatedAt | LocalDateTime | 생성/수정 시간 |

#### Playlist (재생목록)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 재생목록 ID |
| name | String | 이름 |
| description | Text | 설명 |
| thumbnailUrl | String | 썸네일 URL |
| isPublic | Boolean | 공개 여부 |
| createdByUser | User (FK) | 생성한 관리자 |
| createdAt / updatedAt | LocalDateTime | 생성/수정 시간 |

#### PlaylistVideo (재생목록-동영상 연결)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | ID |
| playlist | Playlist (FK) | 재생목록 |
| video | Video (FK) | 동영상 |
| sortOrder | Integer | 재생 순서 |

### 2.2 ER 관계

```
User 1:N → Video (uploaded_by)
User 1:N → Playlist (created_by)
Video N:M → Playlist (via PlaylistVideo, 순서 포함)
```

---

## 3. API 설계

### 3.1 인증 API (`/api/v1/auth`)

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/register` | 회원가입 (기본 VIEWER 역할) | Public |
| POST | `/login` | 로그인 (JWT 발급) | Public |
| POST | `/refresh` | 토큰 갱신 | Authenticated |
| GET | `/me` | 현재 사용자 정보 | Authenticated |

### 3.2 사용자 관리 API (`/api/v1/admin/users`) - SUPER_ADMIN 전용

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| GET | `/` | 사용자 목록 (페이징) | SUPER_ADMIN |
| PATCH | `/{userId}/role` | 역할 변경 | SUPER_ADMIN |
| PATCH | `/{userId}/status` | 상태 변경 (정지/활성) | SUPER_ADMIN |
| DELETE | `/{userId}` | 사용자 삭제 | SUPER_ADMIN |

### 3.3 동영상 관리 API (`/api/v1/admin/videos`) - ADMIN+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/` | 동영상 업로드 (multipart) | ADMIN+ |
| GET | `/` | 관리자 영상 목록 (페이징, 필터, 전체 상태) | ADMIN+ |
| GET | `/{videoId}` | 상세 조회 | ADMIN+ |
| PUT | `/{videoId}` | 메타데이터 수정 | ADMIN+ |
| DELETE | `/{videoId}` | 삭제 (soft delete) | ADMIN+ |
| PATCH | `/{videoId}/visibility` | 공개/비공개 전환 | ADMIN+ |
| GET | `/{videoId}/encoding-status` | 인코딩 진행 상태 | ADMIN+ |

### 3.4 재생목록 관리 API (`/api/v1/admin/playlists`) - ADMIN+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/` | 생성 | ADMIN+ |
| GET | `/` | 목록 | ADMIN+ |
| GET | `/{playlistId}` | 상세 (영상 목록 포함) | ADMIN+ |
| PUT | `/{playlistId}` | 수정 | ADMIN+ |
| DELETE | `/{playlistId}` | 삭제 | ADMIN+ |
| POST | `/{playlistId}/videos` | 영상 추가 | ADMIN+ |
| DELETE | `/{playlistId}/videos/{videoId}` | 영상 제거 | ADMIN+ |
| PUT | `/{playlistId}/videos/reorder` | 순서 변경 | ADMIN+ |

### 3.5 시청자 영상 API (`/api/v1/videos`) - VIEWER+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| GET | `/` | 공개 영상 목록 (페이징, 검색) | VIEWER+ |
| GET | `/{videoId}` | 영상 재생 정보 (HLS URL + 메타데이터) | VIEWER+ |

### 3.6 시청자 재생목록 API (`/api/v1/playlists`) - VIEWER+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| GET | `/` | 공개 재생목록 목록 | VIEWER+ |
| GET | `/{playlistId}` | 재생목록 상세 (영상 순서 포함) | VIEWER+ |

---

## 4. 인증 시스템

- JWT 기반 인증
- Access Token: 30분, 메모리 저장 (프론트엔드)
- Refresh Token: 7일, HttpOnly 쿠키
- 역할 계층: `SUPER_ADMIN > ADMIN > VIEWER`
- 시청자도 로그인 필요

---

## 5. FFmpeg 인코딩

### 5.1 인코딩 명령어

```bash
# 1080p HLS 인코딩
ffmpeg -i /data/videos/originals/input.mp4 \
  -c:v libx264 -preset medium -crf 23 \
  -c:a aac -b:a 128k \
  -hls_time 6 -hls_list_size 0 \
  -hls_segment_filename "/data/videos/encoded/{videoId}/segment_%03d.ts" \
  -f hls /data/videos/encoded/{videoId}/master.m3u8

# 썸네일 추출
ffmpeg -i input.mp4 -ss 00:00:30 -frames:v 1 -q:v 2 \
  /data/videos/thumbnails/{videoId}.jpg
```

### 5.2 인코딩 큐 관리

- 동시 인코딩 최대 1~2개 제한 (CPU 부하 관리)
- 인코딩 대기열은 DB에서 관리 (status: ENCODING_QUEUED → ENCODING → READY)

---

## 6. Nginx 설정

| 역할 | 경로 |
|------|------|
| HLS 정적 파일 서빙 | `/hls/` → `/data/videos/encoded/` |
| 프론트엔드 리버스 프록시 | `/` → Next.js (:3000) |
| 백엔드 API 리버스 프록시 | `/api/` → Spring Boot (:8080) |

---

## 7. 프론트엔드 핵심 로직

### 7.1 HLS 영상 재생

```typescript
// useHlsPlayer.ts
import Hls from 'hls.js';

const hls = new Hls();
hls.loadSource(hlsUrl); // 예: /hls/42/master.m3u8
hls.attachMedia(videoElement);

hls.on(Hls.Events.MANIFEST_PARSED, () => {
  videoElement.play();
});
```

### 7.2 재생목록 자동 재생

```typescript
// PlaylistPlayer 핵심 로직
const [currentIndex, setCurrentIndex] = useState(0);
const videos = playlist.videos; // 순서대로 정렬된 영상 목록

// 현재 영상 종료 시 다음 영상으로 자동 전환
videoElement.onended = () => {
  if (currentIndex < videos.length - 1) {
    setCurrentIndex(currentIndex + 1);
    // 다음 영상 HLS URL 로드
  }
};
```
