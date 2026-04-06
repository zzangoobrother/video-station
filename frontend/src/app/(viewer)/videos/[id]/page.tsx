'use client';

import { useParams } from 'next/navigation';
import { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { apiGet } from '@/lib/api';
import FullscreenPlayer from '@/components/video/FullscreenPlayer';
import type { VideoResponse } from '@/types';

export default function VideoPlayPage() {
  const { id } = useParams<{ id: string }>();
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [video, setVideo] = useState<VideoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<VideoResponse>(`/api/v1/videos/${id}`)
      .then(setVideo)
      .catch((err) => setError(err instanceof Error ? err.message : '영상을 불러올 수 없습니다.'));
  }, [id]);

  useEffect(() => {
    const videoEl = videoRef.current;
    if (!videoEl || !video?.hlsUrl) return;

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;
      hls.loadSource(video.hlsUrl);
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
      videoEl.src = video.hlsUrl;
      videoEl.addEventListener('canplay', () => {
        videoEl.muted = true;
        videoEl.play().catch(() => {});
      }, { once: true });
    }

    return () => {
      if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; }
    };
  }, [video?.hlsUrl]);

  if (!video && !error) {
    return (
      <div className="fixed inset-0 bg-black flex items-center justify-center z-50">
        <div className="w-8 h-8 border-2 border-white/30 border-t-white rounded-full animate-spin" />
      </div>
    );
  }

  return (
    <FullscreenPlayer
      videoRef={videoRef}
      title={video?.title ?? ''}
      subtitle={video?.description ?? undefined}
      error={error}
    />
  );
}
