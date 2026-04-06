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
│       ├── broadcast/                 # Pseudo-Live 방송 로직
│       │   ├── BroadcastStateManager.java
│       │   └── BroadcastScheduler.java
│       ├── websocket/                 # 방송 상태 WebSocket 핸들러
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
│       │   │   ├── playlists/        # 재생목록 목록 / 재생
│       │   │   └── live/             # 라이브 시청
│       │   └── admin/                # 어드민 패널
│       │       ├── videos/           # 동영상 관리
│       │       ├── playlists/        # 재생목록 관리
│       │       ├── broadcast/        # 방송 제어
│       │       └── users/            # 사용자 관리 (최고관리자)
│       ├── components/
│       │   ├── ui/                   # 공통 UI 컴포넌트
│       │   ├── video/                # VideoPlayer (HLS.js)
│       │   ├── playlist/             # PlaylistPlayer, PlaylistEditor 등
│       │   ├── broadcast/            # BroadcastControlPanel 등
│       │   └── layout/               # AdminSidebar, Header 등
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   ├── useBroadcastState.ts  # WebSocket 방송 상태
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

#### Channel (방송 채널)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 채널 ID |
| name | String (unique) | 채널명 |
| description | Text | 설명 |
| thumbnailUrl | String | 썸네일 URL |
| status | Enum | IDLE, LIVE, PAUSED, SCHEDULED |
| owner | User (FK) | 채널 소유 관리자 |
| createdAt / updatedAt | LocalDateTime | 생성/수정 시간 |

#### Broadcast (방송 세션)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 방송 ID |
| channel | Channel (FK) | 방송 채널 |
| playlist | Playlist (FK) | 재생목록 |
| currentVideo | Video (FK) | 현재 재생 중인 영상 |
| currentVideoIndex | Integer | 재생목록 내 현재 인덱스 |
| currentVideoStartedAt | LocalDateTime | 현재 영상 재생 시작 시각 (동기화용) |
| status | Enum | IDLE, LIVE, PAUSED, ENDED |
| startedAt | LocalDateTime | 방송 시작 시간 |
| endedAt | LocalDateTime | 방송 종료 시간 |
| pausedAt | LocalDateTime | 일시정지 시각 |
| totalPausedSeconds | Long | 누적 일시정지 시간 (초) |
| loopPlaylist | Boolean | 재생목록 반복 여부 |
| startedBy | User (FK) | 방송 시작한 관리자 |
| createdAt / updatedAt | LocalDateTime | 생성/수정 시간 |

#### BroadcastSchedule (예약 방송)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 스케줄 ID |
| channel | Channel (FK) | 방송 채널 |
| playlist | Playlist (FK) | 재생목록 |
| scheduledStartAt | LocalDateTime | 예약 시작 시간 |
| scheduledEndAt | LocalDateTime | 예약 종료 시간 |
| recurring | Boolean | 반복 여부 |
| cronExpression | String | 반복 주기 (cron) |
| loopPlaylist | Boolean | 재생목록 반복 여부 |
| status | Enum | PENDING, ACTIVE, COMPLETED, CANCELLED |
| createdByUser | User (FK) | 생성한 관리자 |

### 2.2 ER 관계

```
User 1:N → Video (uploaded_by)
User 1:N → Playlist (created_by)
User 1:N → Channel (owner)
Video N:M → Playlist (via PlaylistVideo, 순서 포함)
Channel 1:N → Broadcast
Channel 1:N → BroadcastSchedule
Broadcast N:1 → Playlist
Broadcast N:1 → Video (current_video)
BroadcastSchedule N:1 → Playlist
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

### 3.7 방송 채널/제어 API (`/api/v1/channels`) - ADMIN+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/` | 채널 생성 | SUPER_ADMIN |
| GET | `/` | 채널 목록 | ADMIN+ |
| GET | `/{channelId}` | 채널 상태 | ADMIN+ |
| POST | `/{channelId}/broadcast/start` | 방송 시작 (playlistId, loopPlaylist 파라미터) | ADMIN+ |
| POST | `/{channelId}/broadcast/stop` | 방송 종료 | ADMIN+ |
| POST | `/{channelId}/broadcast/pause` | 일시정지 | ADMIN+ |
| POST | `/{channelId}/broadcast/resume` | 재개 | ADMIN+ |
| POST | `/{channelId}/broadcast/next` | 다음 영상 | ADMIN+ |
| POST | `/{channelId}/broadcast/previous` | 이전 영상 | ADMIN+ |
| POST | `/{channelId}/broadcast/jump/{index}` | 특정 영상으로 이동 | ADMIN+ |
| GET | `/{channelId}/broadcast/state` | 현재 방송 상태 | ADMIN+ |

### 3.8 예약 방송 API (`/api/v1/channels/{channelId}/schedules`) - ADMIN+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/` | 예약 생성 | ADMIN+ |
| GET | `/` | 예약 목록 | ADMIN+ |
| PUT | `/{scheduleId}` | 예약 수정 | ADMIN+ |
| DELETE | `/{scheduleId}` | 예약 취소 | ADMIN+ |

### 3.9 시청자 라이브 API (`/api/v1/viewer`) - VIEWER+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| GET | `/live` | 현재 방송 정보 (HLS URL + offset + 메타데이터) | VIEWER+ |
| GET | `/vod` | VOD 라이브러리 (페이징) | VIEWER+ |
| GET | `/vod/{videoId}` | VOD 재생 정보 | VIEWER+ |
| GET | `/schedule` | 예약 방송 일정 | VIEWER+ |

### 3.10 WebSocket (`ws://host/ws/broadcast`)

실시간 방송 상태 푸시. 방송 상태 변경 시 연결된 모든 시청자에게 메시지 전송.

**메시지 구조:**
```json
{
  "type": "BROADCAST_STATE",
  "status": "LIVE",
  "currentVideo": {
    "id": 42,
    "title": "영상 제목",
    "hlsUrl": "/hls/42/master.m3u8",
    "durationSeconds": 600,
    "thumbnailUrl": "/hls/thumbnails/42.jpg"
  },
  "offsetSeconds": 127,
  "nextVideo": { "id": 43, "title": "다음 영상", "thumbnailUrl": "..." },
  "currentVideoIndex": 2,
  "totalVideosInPlaylist": 10
}
```

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
| WebSocket 프록시 | `/ws/` → Spring Boot (:8080) |

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

---

## 8. Pseudo-Live 방송 시스템

### 8.1 방송 상태 머신

```
IDLE → LIVE ⇄ PAUSED → ENDED
         │                 ↑
         └─────────────────┘
```

### 8.2 BroadcastStateManager

인메모리 + DB에 방송 상태를 동시 관리하는 핵심 컴포넌트.

- **상태 저장**: 현재 영상 ID, 재생 시작 시각을 인메모리 캐시 + DB에 동시 기록
- **offset 계산**: `현재시각 - currentVideoStartedAt - totalPausedSeconds`
- **일시정지 처리**: pausedAt 기록, 재개 시 totalPausedSeconds에 누적
- **서버 재시작 복구**: 애플리케이션 시작 시 DB에서 LIVE/PAUSED 상태의 방송을 인메모리로 복구

### 8.3 BroadcastScheduler

1초 간격 스케줄러로 자동 영상 전환 처리.

```
현재 영상의 재생 시작 시각 + 영상 길이 < 현재 시각?
→ YES: 다음 영상으로 자동 전환
  - Broadcast 엔티티 갱신 (currentVideo, currentVideoStartedAt)
  - WebSocket으로 전체 시청자에게 전환 알림
→ 재생목록 마지막 영상 끝:
  - loopPlaylist = true → 첫 번째 영상부터 반복
  - loopPlaylist = false → 방송 종료
```

### 8.4 시청자 동기화 메커니즘

```
[시청자 접속 시]
1. GET /api/v1/viewer/live → { videoId, hlsUrl, offsetSeconds }
2. HLS.js로 영상 로드 (Nginx에서 직접)
3. currentTime = offsetSeconds로 설정 (seek)
4. WebSocket 연결 → 영상 전환/상태 변경 실시간 수신
```

### 8.5 WebSocket 방송 상태 훅 (프론트엔드)

```typescript
// useBroadcastState.ts
const useBroadcastState = () => {
  const [state, setState] = useState<BroadcastState | null>(null);
  
  useEffect(() => {
    const ws = new WebSocket(`ws://${host}/ws/broadcast`);
    ws.onmessage = (event) => {
      const data = JSON.parse(event.data);
      setState(data);
    };
    return () => ws.close();
  }, []);
  
  return state;
};
```
