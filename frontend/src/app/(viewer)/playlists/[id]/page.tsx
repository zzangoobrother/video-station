'use client';

import { useParams } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { apiGet } from '@/lib/api';
import { formatDuration } from '@/lib/format';
import FullscreenPlayer from '@/components/video/FullscreenPlayer';
import type { PlaylistDetailResponse } from '@/types';

export default function PlaylistPlayPage() {
  const { id } = useParams<{ id: string }>();
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [playlist, setPlaylist] = useState<PlaylistDetailResponse | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<PlaylistDetailResponse>(`/api/v1/playlists/${id}`)
      .then(setPlaylist)
      .catch((err) => setError(err instanceof Error ? err.message : '재생목록을 불러올 수 없습니다.'));
  }, [id]);

  const currentVideo = playlist?.videos[currentIndex]?.video ?? null;
  const hasNext = playlist ? currentIndex < playlist.videos.length - 1 : false;

  // HLS 로드
  useEffect(() => {
    const videoEl = videoRef.current;
    if (!videoEl || !currentVideo?.hlsUrl) return;

    if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; }

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;
      hls.loadSource(currentVideo.hlsUrl);
      hls.attachMedia(videoEl);
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        videoEl.muted = true;
        videoEl.play().catch(() => {});
      });
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal && data.type === Hls.ErrorTypes.NETWORK_ERROR) hls.startLoad();
        else if (data.fatal && data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
      });
    } else if (videoEl.canPlayType('application/vnd.apple.mpegurl')) {
      videoEl.src = currentVideo.hlsUrl;
      videoEl.addEventListener('canplay', () => {
        videoEl.muted = true;
        videoEl.play().catch(() => {});
      }, { once: true });
    }

    return () => {
      if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; }
    };
  }, [currentVideo?.hlsUrl]);

  // 자동 다음 영상
  useEffect(() => {
    const videoEl = videoRef.current;
    if (!videoEl) return;
    const onEnded = () => { if (hasNext) setCurrentIndex((i) => i + 1); };
    videoEl.addEventListener('ended', onEnded);
    return () => videoEl.removeEventListener('ended', onEnded);
  }, [hasNext]);

  if (!playlist && !error) {
    return (
      <div className="fixed inset-0 bg-black flex items-center justify-center z-50">
        <div className="w-8 h-8 border-2 border-white/30 border-t-white rounded-full animate-spin" />
      </div>
    );
  }

  const subtitle = playlist
    ? `${currentIndex + 1} / ${playlist.videos.length} · ${formatDuration(currentVideo?.durationSeconds)}`
    : undefined;

  const nextVideo = playlist?.videos[currentIndex + 1]?.video;
  const bottomExtra = nextVideo ? (
    <p className="text-white/50 text-sm">다음 영상: {nextVideo.title}</p>
  ) : null;

  return (
    <FullscreenPlayer
      videoRef={videoRef}
      title={currentVideo?.title ?? playlist?.name ?? ''}
      subtitle={subtitle}
      bottomExtra={bottomExtra}
      error={error}
    />
  );
}
