'use client';

import { useEffect, useRef, useState, useCallback } from 'react';
import Hls from 'hls.js';
import { apiGet } from '@/lib/api';
import { formatDuration } from '@/lib/format';
import { useBroadcastState } from '@/hooks/useBroadcastState';
import FullscreenPlayer from '@/components/video/FullscreenPlayer';
import type { BroadcastStateResponse } from '@/types';

export default function LivePage() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const currentVideoIdRef = useRef<number | null>(null);
  const { state: wsState } = useBroadcastState();
  const [initialState, setInitialState] = useState<BroadcastStateResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  const broadcast = wsState ?? initialState;
  const broadcastRef = useRef(broadcast);
  broadcastRef.current = broadcast;

  useEffect(() => {
    apiGet<BroadcastStateResponse>('/api/v1/viewer/live')
      .then(setInitialState)
      .catch(() => setInitialState(null));
  }, []);

  const initHls = useCallback(() => {
    const video = videoRef.current;
    if (!video || hlsRef.current) return;
    if (Hls.isSupported()) {
      const hls = new Hls({ enableWorker: false, maxBufferLength: 30, maxMaxBufferLength: 60 });
      hls.attachMedia(video);
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal && data.type === Hls.ErrorTypes.NETWORK_ERROR) hls.startLoad();
        else if (data.fatal && data.type === Hls.ErrorTypes.MEDIA_ERROR) hls.recoverMediaError();
      });
      hlsRef.current = hls;
    }
  }, []);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !broadcast || !broadcast.currentVideo?.hlsUrl) return;
    if (broadcast.status === 'ENDED' || broadcast.status === 'IDLE') return;

    const videoId = broadcast.currentVideo.id;
    if (currentVideoIdRef.current === videoId) return;
    currentVideoIdRef.current = videoId;

    setError(null);
    const hlsUrl = broadcast.currentVideo.hlsUrl;
    const offset = broadcast.offsetSeconds ?? 0;
    const duration = broadcast.currentVideo.durationSeconds ?? 0;
    const safeOffset = offset < duration ? offset : 0;

    if (Hls.isSupported()) {
      initHls();
      const hls = hlsRef.current!;
      hls.loadSource(hlsUrl);
      const onManifest = () => {
        video.currentTime = safeOffset;
        video.muted = true;
        video.play().catch(() => {});
        hls.off(Hls.Events.MANIFEST_PARSED, onManifest);
      };
      hls.on(Hls.Events.MANIFEST_PARSED, onManifest);
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = hlsUrl;
      video.addEventListener('loadedmetadata', () => {
        video.currentTime = safeOffset;
        video.muted = true;
        video.play().catch(() => {});
      }, { once: true });
    }
  }, [broadcast?.currentVideo?.id, broadcast?.status, initHls]);

  useEffect(() => {
    const video = videoRef.current;
    if (!video || !broadcast) return;
    if (broadcast.status === 'PAUSED') video.pause();
    else if (broadcast.status === 'LIVE' && video.paused) video.play().catch(() => {});
  }, [broadcast?.status]);

  useEffect(() => {
    return () => { if (hlsRef.current) { hlsRef.current.destroy(); hlsRef.current = null; } };
  }, []);

  if (!broadcast || broadcast.status === 'IDLE' || broadcast.status === 'ENDED') {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-gray-900 flex items-center justify-center aspect-video rounded">
          <div className="text-center">
            <p className="text-gray-400 text-lg">현재 방송 중인 채널이 없습니다.</p>
            <p className="text-gray-500 text-sm mt-2">방송이 시작되면 자동으로 재생됩니다.</p>
          </div>
        </div>
      </div>
    );
  }

  const liveBadge = (
    <div className="flex items-center gap-2">
      <span className={`px-2 py-1 rounded text-xs font-bold ${
        broadcast.status === 'LIVE' ? 'bg-red-500 text-white' : 'bg-yellow-400 text-gray-900'
      }`}>
        {broadcast.status === 'LIVE' ? 'LIVE' : '일시정지'}
      </span>
      <span className="text-white/70 text-sm">{broadcast.playlistName}</span>
    </div>
  );

  const subtitle = `${broadcast.currentVideoIndex + 1} / ${broadcast.totalVideosInPlaylist} · ${formatDuration(broadcast.currentVideo?.durationSeconds)}`;

  const bottomExtra = (
    <>
      {broadcast.nextVideo && (
        <p className="text-white/50 text-sm">다음 영상: {broadcast.nextVideo.title}</p>
      )}
      {broadcast.status === 'PAUSED' && (
        <p className="text-yellow-400 text-sm mt-1">방송이 일시정지되었습니다.</p>
      )}
    </>
  );

  return (
    <FullscreenPlayer
      videoRef={videoRef}
      title={broadcast.currentVideo?.title ?? ''}
      subtitle={subtitle}
      badge={liveBadge}
      bottomExtra={bottomExtra}
      error={error}
    />
  );
}
