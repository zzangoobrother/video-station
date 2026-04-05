# Video Station

VOD 스트리밍 서비스 - 어드민이 동영상을 업로드하고 재생목록을 구성하여 시청자가 시청하는 서비스

## 문서

- [prd-video.md](./prd-video.md) - 제품 요구사항
- [spec.md](./spec.md) - 구현 스펙 (DB 스키마, API, 프로젝트 구조)
- [tasks.md](./tasks.md) - 구현 계획 및 진행 상황
- [backend/openapi.yml](./backend/openapi.yml) - API 스펙

## 기술 스택

- **Backend**: Spring Boot 4.0.5, Java 25, JPA, MySQL 8, Gradle Kotlin DSL
- **Frontend**: Next.js 16 (App Router), React 19, TypeScript, Tailwind CSS 4
- **인프라**: Docker Compose (MySQL), Nginx (HLS 서빙 + 리버스 프록시)

## 프로젝트 구조

```
video-station/
├── backend/          # Spring Boot
│   ├── docker/       # docker-compose.yml (MySQL)
│   ├── nginx/        # nginx.conf
│   └── src/main/java/com/videostation/
│       ├── api/           # REST Controller
│       ├── application/   # Service + DTO
│       ├── domain/        # Entity + Enum
│       ├── persistence/   # Repository
│       ├── global/        # Security, Config, Error, Filter
│       ├── encoding/      # FFmpeg 인코딩
│       └── event/         # 도메인 이벤트
└── frontend/         # Next.js
    └── src/
        ├── app/           # 페이지 (App Router)
        ├── components/    # UI 컴포넌트
        ├── hooks/         # Custom hooks
        ├── lib/           # API 클라이언트
        └── types/         # TypeScript 타입
```

## 코드 컨벤션

### Entity
- Lombok은 `@Getter`, `@NoArgsConstructor(access = AccessLevel.PROTECTED)` 만 사용
- `@Builder`, `@AllArgsConstructor`, `@Setter` 사용 금지
- 객체 생성은 `static create()` 팩토리 메서드
- `BaseEntity` 상속 (createdAt, updatedAt 자동 관리)

### DTO
- Java `record` 사용
- 요청 DTO: validation 어노테이션 (`@NotBlank`, `@Email`, `@Size` 등)
- 응답 DTO: `static from(Entity)` 메서드로 변환

### Service
- `@RequiredArgsConstructor`로 생성자 주입
- 클래스 레벨 `@Transactional(readOnly = true)`, 쓰기 메서드만 `@Transactional` 오버라이드
- 비즈니스 예외는 `BusinessException(ErrorCode)` 사용

### 테스트 (TDD)
- 테스트 먼저 작성 → 실패 확인(RED) → 구현(GREEN) → 리팩터
- JUnit 5 + Mockito + AssertJ
- `@DisplayName` 한국어로 작성
- 단위 테스트: `@ExtendWith(MockitoExtension.class)`, `@Mock`, `@InjectMocks`
- 컨트롤러 테스트: `@WebMvcTest` + `@Import(SecurityConfig.class)`
- 테스트에서 id 설정: `ReflectionTestUtils.setField(entity, "id", 1L)`

### 에러 처리
- `ErrorCode` enum: HttpStatus + 코드 + 메시지
- `BusinessException` → `GlobalExceptionHandler`에서 일괄 처리
- 응답 형태: `{ status, code, message }`

## 빌드 & 실행

```bash
# MySQL 실행
cd backend/docker && docker compose up -d

# 백엔드 테스트
cd backend && ./gradlew test

# 백엔드 실행
cd backend && ./gradlew bootRun

# 프론트엔드 실행
cd frontend && npm run dev
```
