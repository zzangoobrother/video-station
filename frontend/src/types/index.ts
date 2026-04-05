// === 공통 ===
export interface ErrorResponse {
  status: number;
  code: string;
  message: string;
}

export interface PageResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

// === 인증 ===
export interface RegisterRequest {
  email: string;
  password: string;
  name: string;
  nickname: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface TokenResponse {
  accessToken: string;
  expiresIn: number;
}

export interface UserResponse {
  id: number;
  email: string;
  name: string;
  nickname: string;
  role: 'SUPER_ADMIN' | 'ADMIN' | 'VIEWER';
  status: 'ACTIVE' | 'INACTIVE' | 'BANNED';
  profileImageUrl: string | null;
  createdAt: string;
}

// === 동영상 ===
export type VideoStatus = 'UPLOADING' | 'ENCODING_QUEUED' | 'ENCODING' | 'READY' | 'FAILED' | 'DELETED';

export interface VideoResponse {
  id: number;
  title: string;
  description: string;
  originalFileName: string;
  fileSize: number;
  thumbnailUrl: string | null;
  durationSeconds: number | null;
  status: VideoStatus;
  hlsUrl: string | null;
  tags: string;
  isPublic: boolean;
  viewCount: number;
  uploadedBy: UserResponse;
  createdAt: string;
}

// === 재생목록 ===
export interface PlaylistResponse {
  id: number;
  name: string;
  description: string;
  thumbnailUrl: string | null;
  isPublic: boolean;
  videoCount: number;
  totalDurationSeconds: number;
  createdBy: UserResponse;
  createdAt: string;
}

export interface PlaylistVideoResponse {
  sortOrder: number;
  video: VideoResponse;
}

export interface PlaylistDetailResponse extends PlaylistResponse {
  videos: PlaylistVideoResponse[];
}
