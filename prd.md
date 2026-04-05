# Video Station - 동영상 스트리밍 서비스 PRD

## 1. 개요

어드민이 동영상을 업로드하고, 재생 목록을 구성하여 방문자에게 TV 방송처럼 순서대로 송출하는 스트리밍 서비스.
핵심 컨셉은 **Pseudo-Live** - 미리 업로드된 동영상을 어드민이 제어하여 라이브처럼 송출하는 것이다.

- 어드민이 동영상을 업로드하면 해당 동영상을 볼 수 있음
- 최고 관리자(Super Admin)가 동영상을 제어할 수 있음
- 제어란: 어드민이 웹사이트 방문자에게 어떤 동영상을 볼지 컨트롤하는 것
- 라이브 방송처럼 재생목록의 동영상을 순서대로 송출

### 운영 제약

| 항목 | 값 |
|------|-----|
| 최대 동시 시청자 | **500명** |
| 하루 평균 방송 시간 | **8시간 이상** |
| 기본 영상 화질 | **1080p** (5Mbps) |
| 인프라 비용 목표 | **최소 비용 운영** |

---

## 2. 기술 스택

| 구분 | 기술 |
|------|------|
| Backend | Spring Boot 4.0.5, Java 25, JPA, MySQL, Lombok |
| Frontend | Next.js (App Router), React, TypeScript, Tailwind CSS |
| 서버 | NCP Server (VM) 1대 |
| 저장소 | 서버 로컬 디스크 (HLS 서빙) + NCP Object Storage (원본 백업) |
| Build | Gradle (Kotlin DSL) |
| 인증 | JWT (Access Token + Refresh Token) |
| 실시간 통신 | WebSocket (방송 상태 푸시) |
| 영상 재생 | HLS.js + p2p-media-loader (P2P CDN) |
| 영상 인코딩 | FFmpeg (서버 인코딩, 무료) |
| 웹 서버 | Nginx (HLS 정적 파일 서빙) |
| 스트리밍 방식 | Pseudo-Live (백엔드 상태 관리 + 시청자 동기화) |

### 비용 절감을 위해 사용하지 않는 서비스

| 서비스 | 미사용 이유 | 대체 방안 |
|--------|------------|-----------|
| NCP Live Station | 채널 운영비 발생, 500명에 과도한 인프라 | Pseudo-Live (백엔드 상태 관리) |
| NCP VOD Station | 인코딩 분당 과금 | FFmpeg (무료) |
| NCP CDN+ / Global CDN | 전송 비용 발생 | Nginx 직접 서빙 + P2P 오프로드 |

---

## 3. 인프라 구성

### 3.1 단일 서버 아키텍처

```
┌─────────────────────────────────────────────────────┐
│              NCP Server (VM) 1대                     │
│                                                      │
│  ┌─────────────┐  ┌──────────────┐  ┌────────────┐  │
│  │ Nginx       │  │ Spring Boot  │  │ MySQL      │  │
│  │ (HLS 서빙)  │  │ (API+WS)    │  │ (Docker)   │  │
│  │ :80/:443    │  │ :8080        │  │ :3306      │  │
│  └──────┬──────┘  └──────┬───────┘  └────────────┘  │
│         │                │                           │
│  ┌──────┴────────────────┴───────┐                   │
│  │      로컬 디스크 / Block Storage  │                 │
│  │  /data/videos/encoded/  (HLS)  │                  │
│  │  /data/videos/originals/ (원본) │                  │
│  │  /data/videos/thumbnails/      │                  │
│  └────────────────────────────────┘                  │
│                                                      │
│  FFmpeg (업로드 시 비동기 인코딩)                       │
└──────────────────────┬───────────────────────────────┘
                       │
              인터넷 (1Gbps+)
                       │
          ┌────────────┼────────────┐
          ▼            ▼            ▼
       시청자 A     시청자 B     시청자 C
          └────── P2P (WebRTC) ──────┘
            시청자끼리 HLS 세그먼트 공유
            서버 대역폭 70% 절감
```

### 3.2 Nginx 설정 역할

| 역할 | 경로 |
|------|------|
| HLS 정적 파일 서빙 | `/hls/` → `/data/videos/encoded/` |
| 프론트엔드 리버스 프록시 | `/` → Next.js (:3000) |
| 백엔드 API 리버스 프록시 | `/api/` → Spring Boot (:8080) |
| WebSocket 프록시 | `/ws/` → Spring Boot (:8080) |

### 3.3 NCP Object Storage 역할 (백업용만)

- 원본 영상 파일 백업 저장 (서버 장애 대비)
- 서빙 용도로는 사용하지 않음 → **전송 비용 0원**
- 저장 비용만 발생: ~23원/GB/월

### 3.4 서버 사양 권장

| 항목 | 최소 사양 | 권장 사양 |
|------|----------|----------|
| CPU | 4 vCPU | 8 vCPU (인코딩 시 CPU 사용) |
| RAM | 8GB | 16GB |
| 디스크 | 500GB SSD | 1TB SSD (영상 파일 저장) |
| 네트워크 | 1Gbps | 2Gbps (여유분) |

---

## 4. 아키텍처: Pseudo-Live 방식

### 4.1 핵심 개념

실제 라이브 스트리밍 인프라 없이, 백엔드가 "지금 어떤 영상의 몇 초 지점을 재생 중인지" 상태를 관리하고, 모든 시청자가 이 상태에 맞춰 동기화된 재생을 하는 방식.

```
[어드민] 재생목록 선택 → "방송 시작" 클릭
                       │ REST API
                       ▼
[Backend: BroadcastStateManager]
  - 현재 영상 ID, 재생 시작 시각 기록
  - 1초 간격 스케줄러로 영상 종료 감지 → 자동 다음 영상 전환
  - 시청자 요청 시: "video_42.m3u8, offset 127초부터 재생" 응답
                       │ WebSocket + REST
                       ▼
[시청자 브라우저]
  1. WebSocket으로 방송 상태 수신
  2. HLS.js가 Nginx에서 HLS 세그먼트 로드
  3. 서버가 알려준 offset부터 재생 → 전원 동기화 (±1~3초)
  4. P2P로 시청자끼리 세그먼트 공유 → 서버 부하 70% 감소
```

### 4.2 동기화 메커니즘

```
[방송 시작 시]
Backend 저장:
  - broadcastStartedAt = 2026-04-05T14:00:00
  - playlist = [영상A(600초), 영상B(300초), 영상C(900초)]

[시청자 접속 시 (14:12:30)]
Backend 계산:
  - 경과시간 = 750초
  - 영상A(600초) 완료 → 영상B에서 150초 지점
  - 응답: { videoId: B, hlsUrl: "/hls/video_B/master.m3u8", offsetSeconds: 150 }

[시청자 브라우저]
  - HLS.js로 영상B 로드 (Nginx에서 직접)
  - currentTime = 150으로 설정 (seek)
  - 재생 시작 → 모든 시청자가 ±1~3초 내 동기화
```

### 4.3 자동 영상 전환

```
Backend 스케줄러 (1초 간격):
  현재 영상의 재생 시작 시각 + 영상 길이 < 현재 시각?
  → YES: 다음 영상으로 자동 전환
    - Broadcast 엔티티 갱신 (currentVideo, currentVideoStartedAt)
    - WebSocket으로 전체 시청자에게 전환 알림
  → 재생목록 마지막 영상 끝:
    - loopPlaylist = true → 첫 번째 영상부터 반복
    - loopPlaylist = false → 방송 종료
```

---

## 5. 아키텍처: P2P CDN

### 5.1 동작 원리

모든 시청자가 같은 영상을 같은 시점에 시청하므로, WebRTC를 통해 시청자끼리 HLS 세그먼트를 공유. 서버 대역폭 60~80% 절감.

```
시청자 A ──┐
           ├── WebRTC로 HLS 세그먼트 공유
시청자 B ──┘
           │
시청자 C ──── 못 받은 세그먼트만 서버(Nginx)에서 다운로드
```

### 5.2 기술 구성

- **라이브러리**: p2p-media-loader (MIT 라이선스, 오픈소스)
- HLS.js 플러그인으로 동작
- **트래커**: p2p-media-loader 내장 WebSocket 트래커
- **폴백**: P2P 실패 시 자동으로 서버에서 직접 다운로드

### 5.3 대역폭 절감 효과

```
P2P 없이:
  500명 × 5Mbps = 2.5Gbps (서버 감당 불가)

P2P 적용 (70% 오프로드):
  서버 부담: 2.5Gbps × 30% = 750Mbps
  → 1Gbps 서버로 충분히 감당
```

---

## 6. 아키텍처: FFmpeg 인코딩

### 6.1 인코딩 흐름

```
어드민 업로드 (원본 mp4)
  → Backend 수신 (multipart)
  → 서버 로컬 디스크에 원본 저장 (/data/videos/originals/)
  → (비동기) Object Storage에 원본 백업
  → Video 엔티티 저장 (status: UPLOADING → ENCODING)
  → FFmpeg 비동기 실행 (별도 스레드)
      → 1080p HLS 세그먼트 생성 (.m3u8 + .ts)
      → /data/videos/encoded/{videoId}/ 에 저장
      → 썸네일 자동 추출 → /data/videos/thumbnails/
  → Video 엔티티 갱신 (status: READY, hlsUrl 설정)
  → Nginx가 /hls/{videoId}/ 경로로 자동 서빙
```

### 6.2 FFmpeg 명령어

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

# 소요시간: 1시간 영상 ≈ 10~30분
# 비용: 0원
```

### 6.3 인코딩 큐 관리

- 동시 인코딩 최대 1~2개 제한 (CPU 부하 관리)
- 인코딩 대기열은 DB에서 관리 (status: ENCODING_QUEUED → ENCODING → READY)
- 방송 중에는 인코딩 우선순위 낮춤

---

## 7. 프로젝트 구조

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
│       │   ├── config/              # Security, JPA, WebSocket, Swagger
│       │   ├── error/               # GlobalExceptionHandler
│       │   └── filter/              # TokenAuthenticationFilter
│       ├── encoding/                  # FFmpeg 인코딩 처리
│       │   ├── FFmpegEncoder.java
│       │   ├── EncodingQueue.java    # 인코딩 대기열 관리
│       │   └── EncodingEventListener.java
│       ├── broadcast/                 # Pseudo-Live 방송 로직
│       │   ├── BroadcastStateManager.java
│       │   └── BroadcastScheduler.java
│       ├── event/                     # 도메인 이벤트
│       └── websocket/                 # 방송 상태 WebSocket 핸들러
│
├── frontend/                          # Next.js
│   ├── package.json
│   ├── next.config.ts
│   └── src/
│       ├── app/
│       │   ├── (auth)/               # 로그인, 회원가입
│       │   ├── (viewer)/             # 시청자 페이지
│       │   │   ├── live/             # 라이브 시청
│       │   │   └── vod/              # VOD 라이브러리
│       │   └── admin/                # 어드민 패널
│       │       ├── videos/           # 동영상 관리
│       │       ├── playlists/        # 재생목록 관리
│       │       ├── broadcast/        # 방송 제어
│       │       └── users/            # 사용자 관리 (최고관리자)
│       ├── components/
│       │   ├── ui/                   # 공통 UI 컴포넌트
│       │   ├── video/                # VideoPlayer (HLS.js + P2P)
│       │   ├── playlist/             # PlaylistEditor 등
│       │   ├── broadcast/            # BroadcastControlPanel 등
│       │   └── layout/               # AdminSidebar, Header 등
│       ├── hooks/
│       │   ├── useAuth.ts
│       │   ├── useBroadcastState.ts  # WebSocket 방송 상태
│       │   └── useHlsPlayer.ts       # HLS.js + P2P 초기화
│       ├── lib/                       # API 클라이언트, 상수
│       └── types/                     # TypeScript 타입 정의
└── prd.md
```

---

## 8. 사용자 역할

| 역할 | 설명 |
|------|------|
| **SUPER_ADMIN** | 최고 관리자. 모든 기능 접근 가능. 다른 관리자 관리, 채널 생성 |
| **ADMIN** | 관리자. 동영상 업로드, 재생목록 관리, 방송 제어 |
| **VIEWER** | 시청자. 로그인 후 라이브 스트림 및 VOD 시청 |

---

## 9. 데이터베이스 스키마

### 9.1 핵심 엔티티

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
| vodPublic | Boolean | VOD 라이브러리 공개 여부 |
| uploadedBy | User (FK) | 업로드한 관리자 |
| createdAt / updatedAt | LocalDateTime | 생성/수정 시간 |

#### VideoEncodingProfile (인코딩 프로필)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | ID |
| video | Video (FK) | 동영상 |
| resolution | String | 해상도 (1080p, 720p, 480p) |
| bitrate | Integer | 비트레이트 (kbps) |
| hlsPath | String | 인코딩된 HLS 파일 로컬 경로 |
| status | Enum | PENDING, PROCESSING, COMPLETED, FAILED |

#### Playlist (재생목록)
| 필드 | 타입 | 설명 |
|------|------|------|
| id | Long (PK) | 재생목록 ID |
| name | String | 이름 |
| description | Text | 설명 |
| thumbnailUrl | String | 썸네일 URL |
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

### 9.2 ER 관계

```
User 1:N → Video (uploaded_by)
User 1:N → Playlist (created_by)
User 1:N → Channel (owner)
Video 1:N → VideoEncodingProfile
Video N:M → Playlist (via PlaylistVideo, 순서 포함)
Channel 1:N → Broadcast
Channel 1:N → BroadcastSchedule
Broadcast N:1 → Playlist
Broadcast N:1 → Video (current_video)
BroadcastSchedule N:1 → Playlist
```

---

## 10. API 설계

### 10.1 인증 API (`/api/v1/auth`)

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/register` | 회원가입 (기본 VIEWER 역할) | Public |
| POST | `/login` | 로그인 (JWT 발급) | Public |
| POST | `/refresh` | 토큰 갱신 | Authenticated |
| GET | `/me` | 현재 사용자 정보 | Authenticated |

### 10.2 사용자 관리 API (`/api/v1/admin/users`) - SUPER_ADMIN 전용

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| GET | `/` | 사용자 목록 (페이징) | SUPER_ADMIN |
| PATCH | `/{userId}/role` | 역할 변경 | SUPER_ADMIN |
| PATCH | `/{userId}/status` | 상태 변경 (정지/활성) | SUPER_ADMIN |
| DELETE | `/{userId}` | 사용자 삭제 | SUPER_ADMIN |

### 10.3 동영상 API (`/api/v1/videos`) - ADMIN+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/` | 동영상 업로드 (multipart) | ADMIN+ |
| GET | `/` | 목록 (페이징, 필터) | ADMIN+ |
| GET | `/{videoId}` | 상세 조회 | ADMIN+ |
| PUT | `/{videoId}` | 메타데이터 수정 | ADMIN+ |
| DELETE | `/{videoId}` | 삭제 (soft delete) | ADMIN+ |
| PATCH | `/{videoId}/visibility` | 공개/비공개 전환 | ADMIN+ |
| GET | `/{videoId}/encoding-status` | 인코딩 진행 상태 | ADMIN+ |

### 10.4 재생목록 API (`/api/v1/playlists`) - ADMIN+

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

### 10.5 방송 제어 API (`/api/v1/channels`) - ADMIN+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/` | 채널 생성 | SUPER_ADMIN |
| GET | `/` | 채널 목록 | ADMIN+ |
| GET | `/{channelId}` | 채널 상태 | ADMIN+ |
| POST | `/{channelId}/broadcast/start` | 방송 시작 | ADMIN+ |
| POST | `/{channelId}/broadcast/stop` | 방송 종료 | ADMIN+ |
| POST | `/{channelId}/broadcast/pause` | 일시정지 | ADMIN+ |
| POST | `/{channelId}/broadcast/resume` | 재개 | ADMIN+ |
| POST | `/{channelId}/broadcast/next` | 다음 영상 | ADMIN+ |
| POST | `/{channelId}/broadcast/previous` | 이전 영상 | ADMIN+ |
| POST | `/{channelId}/broadcast/jump/{index}` | 특정 영상으로 이동 | ADMIN+ |
| GET | `/{channelId}/broadcast/state` | 현재 방송 상태 | ADMIN+ |

### 10.6 예약 방송 API (`/api/v1/channels/{channelId}/schedules`) - ADMIN+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| POST | `/` | 예약 생성 | ADMIN+ |
| GET | `/` | 예약 목록 | ADMIN+ |
| PUT | `/{scheduleId}` | 예약 수정 | ADMIN+ |
| DELETE | `/{scheduleId}` | 예약 취소 | ADMIN+ |

### 10.7 시청자 API (`/api/v1/viewer`) - VIEWER+

| Method | Path | 설명 | 접근 권한 |
|--------|------|------|-----------|
| GET | `/live` | 현재 방송 정보 (HLS URL + offset + 메타데이터) | VIEWER+ |
| GET | `/vod` | VOD 라이브러리 (페이징) | VIEWER+ |
| GET | `/vod/{videoId}` | VOD 재생 정보 | VIEWER+ |
| GET | `/schedule` | 예약 방송 일정 | VIEWER+ |

### 10.8 WebSocket

| Path | 설명 |
|------|------|
| `ws://host/ws/broadcast` | 실시간 방송 상태 푸시 |

**WebSocket 메시지 구조:**
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

## 11. 핵심 기능 상세

### 11.1 방송 제어 (핵심 기능)

**방송 상태 머신:**
```
IDLE → LIVE ⇄ PAUSED → ENDED
         │                 ↑
         └─────────────────┘
```

**Pseudo-Live 상태 관리 (BroadcastStateManager):**
- 현재 영상 ID, 재생 시작 시각을 인메모리 + DB에 동시 관리
- 1초 간격 스케줄러로 영상 종료 감지 → 자동 다음 영상 전환
- 일시정지 시: pausedAt 기록, 재개 시: totalPausedSeconds에 누적
- offset 계산: `현재시각 - currentVideoStartedAt - totalPausedSeconds`
- 서버 재시작 시: DB에서 방송 상태 복구

### 11.2 인증 시스템

- JWT 기반 인증
- Access Token: 30분, 메모리 저장 (프론트엔드)
- Refresh Token: 7일, HttpOnly 쿠키
- 역할 계층: `SUPER_ADMIN > ADMIN > VIEWER`
- 시청자도 로그인 필요

### 11.3 P2P 영상 재생 (프론트엔드)

```typescript
// useHlsPlayer.ts 핵심 로직
import Hls from 'hls.js';
import { HlsJsP2PEngine } from 'p2p-media-loader-hlsjs';

const HlsWithP2P = HlsJsP2PEngine.injectMixin(Hls);
const hls = new HlsWithP2P({
  p2p: {
    core: {
      swarmId: 'video-station-broadcast',
    },
  },
});

hls.on(Hls.Events.MANIFEST_PARSED, () => {
  videoElement.currentTime = offsetSeconds; // 서버에서 받은 offset
  videoElement.play();
});
```

---

## 12. 비용 추정

### 12.1 월간 비용 (500명 동접, 8시간/일, 1080p 기준)

| 항목 | 비용 |
|------|------|
| NCP Server (8vCPU, 16GB, 1TB SSD) | ~20~40만원/월 |
| NCP 서버 아웃바운드 전송 (81TB, P2P 70% 오프로드) | 서버 요금제에 따라 다름 |
| NCP Object Storage 저장 (원본 백업 500GB) | ~1.2만원/월 |
| Object Storage 전송 | **0원** (서빙 안 함) |
| CDN | **0원** (사용 안 함) |
| Live Station | **0원** (사용 안 함) |
| VOD Station | **0원** (FFmpeg 대체) |
| 도메인 + SSL | ~1~2만원/월 |

### 12.2 핵심 변수: NCP 서버 아웃바운드 전송비

서버 직접 서빙의 비용은 NCP의 서버 아웃바운드 트래픽 과금 정책에 따라 결정됨.
배포 전 NCP 요금 계산기로 정확한 비용 확인 필요.

**만약 서버 전송비가 비싸다면:**
- CDN+ 도입 (GB당 단가가 더 낮을 수 있음)
- 또는 Cloudflare 무료 플랜 (대역폭 무제한) 프록시 검토

### 12.3 이전 방식 대비 절감

| 방식 | 월 예상 비용 |
|------|-------------|
| Live Station + CDN+ (초기 안) | ~1,400만원+ |
| CDN+ + P2P (이전 안) | ~425만원 |
| **서버 직접 서빙 + P2P (현재 안)** | **서버 비용 + 전송비만** |

---

## 13. 주의사항 및 제약

- **시청자 동기화 오차**: Pseudo-Live 방식은 시청자 간 1~3초 오차 발생 가능
- **서버 대역폭**: P2P 70% 오프로드 기준 750Mbps 필요. 1Gbps 서버 권장
- **디스크 용량**: 인코딩된 HLS 파일이 원본 대비 1.5~2배 용량 차지. 디스크 모니터링 필요
- **FFmpeg 인코딩 부하**: 동시 인코딩 1~2개 제한. 방송 중에는 인코딩 우선순위 낮춤
- **서버 재시작 시**: 인메모리 방송 상태 DB에서 복구 로직 필수
- **P2P 방화벽**: 일부 기업 네트워크에서 WebRTC 차단 가능 → 자동 서버 폴백
- **HLS 세그먼트 크기**: 6초 권장
- **서버 단일 장애점**: 서버 1대 구성이므로 장애 시 서비스 중단. 필요 시 이중화 고려

---

## 14. 구현 순서

### Phase 1: 프로젝트 기반 구축
- Spring Boot 4 + Gradle Kotlin DSL 프로젝트 생성
- Next.js 프로젝트 생성
- Docker Compose (MySQL)
- Nginx 설정 (리버스 프록시 + HLS 정적 파일 서빙)
- JPA 엔티티 전체 구현
- JWT 인증 시스템
- SecurityConfig

### Phase 2: 동영상 관리
- 동영상 업로드 (로컬 디스크 저장)
- FFmpeg 인코딩 파이프라인 (비동기 + 큐 관리)
- Object Storage 원본 백업 (비동기)
- 동영상 CRUD API
- 프론트엔드: 동영상 업로드, 목록, 인코딩 상태

### Phase 3: 재생목록
- 재생목록 CRUD API
- 드래그앤드롭 순서 변경
- 프론트엔드: 재생목록 에디터

### Phase 4: 방송 시스템 (Pseudo-Live)
- BroadcastStateManager (인메모리 + DB 상태 관리)
- BroadcastScheduler (자동 영상 전환)
- 방송 제어 API
- WebSocket 실시간 상태 푸시
- 프론트엔드: 방송 제어 패널

### Phase 5: 시청자 경험
- 시청자 API (방송 상태 + offset 제공)
- HLS.js + P2P(p2p-media-loader) 비디오 플레이어
- 라이브 시청 페이지 (동기화 재생)
- VOD 라이브러리 페이지

### Phase 6: 관리 및 마무리
- 최고관리자 패널 (사용자 관리)
- 방송 예약 기능
- 에러 핸들링, 로깅
- Docker 컨테이너화 (전체 스택)

---

## 15. 검증 방법

1. **단위 테스트**: BroadcastStateManager offset 계산, 자동 전환, 일시정지/재개 로직
2. **통합 테스트**: API 엔드포인트 테스트 (MockMvc / TestRestClient)
3. **수동 테스트**:
   - 동영상 업로드 → FFmpeg 인코딩 → Nginx에서 HLS 재생 확인
   - 방송 시작 → 시청자 화면에서 동기화된 재생 확인
   - 어드민 영상 전환 → 시청자 1~3초 내 반영 확인
   - 일시정지 → 재개 시 올바른 offset 확인
4. **P2P 테스트**: 브라우저 개발자 도구에서 WebRTC 트래픽 확인
5. **부하 테스트**: 500명 동시 접속 시뮬레이션 (k6 또는 Artillery)
