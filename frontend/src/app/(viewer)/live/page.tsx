'use client';

import { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';
import { apiGet } from '@/lib/api';
import { formatDuration } from '@/lib/format';
import { useBroadcastState } from '@/hooks/useBroadcastState';
import type { BroadcastStateResponse } from '@/types';

export default function LivePage() {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const { state: wsState } = useBroadcastState();
  const [initialState, setInitialState] = useState<BroadcastStateResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  // 현재 표시 상태: WebSocket 상태가 있으면 우선, 없으면 초기 상태
  const broadcast = wsState ?? initialState;

  // 초기 방송 상태 로드
  useEffect(() => {
    apiGet<BroadcastStateResponse>('/api/v1/viewer/live')
      .then(setInitialState)
      .catch(() => setInitialState(null));
  }, []);

  // HLS 로드 + offset seek
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !broadcast || !broadcast.currentVideo?.hlsUrl) return;
    if (broadcast.status === 'ENDED' || broadcast.status === 'IDLE') return;

    // 이전 인스턴스 정리
    if (hlsRef.current) {
      hlsRef.current.destroy();
      hlsRef.current = null;
    }

    const hlsUrl = broadcast.currentVideo.hlsUrl;

    const offset = broadcast.offsetSeconds;
    const isLive = broadcast.status === 'LIVE';

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;
      hls.loadSource(hlsUrl);
      hls.attachMedia(video);
      hls.on(Hls.Events.MANIFEST_PARSED, () => {
        video.currentTime = offset;
        if (isLive) {
          video.play().catch(() => {});
        }
      });
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) setError('영상 로드에 실패했습니다.');
      });
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = hlsUrl;
      video.addEventListener('loadedmetadata', () => {
        video.currentTime = offset;
        if (isLive) {
          video.play().catch(() => {});
        }
      }, { once: true });
    } else {
      setError('이 브라우저에서는 HLS 재생을 지원하지 않습니다.');
    }

    return () => {
      if (hlsRef.current) {
        hlsRef.current.destroy();
        hlsRef.current = null;
      }
    };
    // status 변경은 별도 이펙트에서 처리 - HLS 재생성 불필요
  }, [broadcast?.currentVideo?.id]);

  // 일시정지/재개 처리
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !broadcast) return;
    if (broadcast.status === 'PAUSED') {
      video.pause();
    } else if (broadcast.status === 'LIVE' && video.paused) {
      video.play().catch(() => {});
    }
  }, [broadcast?.status]);

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

  if (error) {
    return (
      <div className="max-w-4xl mx-auto">
        <div className="bg-black text-white flex items-center justify-center aspect-video rounded">
          {error}
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-4xl mx-auto">
      {/* 상태 배지 */}
      <div className="flex items-center gap-2 mb-3">
        <span className={`px-2 py-1 rounded text-xs font-bold ${
          broadcast.status === 'LIVE' ? 'bg-red-500 text-white' : 'bg-yellow-400 text-gray-900'
        }`}>
          {broadcast.status === 'LIVE' ? 'LIVE' : '일시정지'}
        </span>
        <span className="text-sm text-gray-500">{broadcast.playlistName}</span>
      </div>

      {/* 비디오 플레이어 */}
      <video
        ref={videoRef}
        className="w-full aspect-video bg-black rounded"
        controls={false}
      />

      {/* 영상 정보 */}
      <div className="mt-4">
        <h2 className="text-xl font-bold">{broadcast.currentVideo?.title}</h2>
        <p className="text-sm text-gray-500 mt-1">
          {broadcast.currentVideoIndex + 1} / {broadcast.totalVideosInPlaylist}
          {' · '}
          {formatDuration(broadcast.currentVideo?.durationSeconds)}
        </p>
        {broadcast.nextVideo && (
          <p className="text-sm text-gray-400 mt-2">
            다음 영상: {broadcast.nextVideo.title}
          </p>
        )}
      </div>

      {broadcast.status === 'PAUSED' && (
        <div className="mt-4 bg-yellow-50 border border-yellow-200 rounded p-3 text-sm text-yellow-700">
          방송이 일시정지되었습니다. 잠시 기다려주세요.
        </div>
      )}
    </div>
  );
}
