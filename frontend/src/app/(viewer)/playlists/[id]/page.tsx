'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';
import { formatDuration } from '@/lib/format';
import VideoPlayer from '@/components/video/VideoPlayer';
import type { PlaylistDetailResponse } from '@/types';

export default function PlaylistPlayPage() {
  const { id } = useParams<{ id: string }>();
  const [playlist, setPlaylist] = useState<PlaylistDetailResponse | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<PlaylistDetailResponse>(`/api/v1/playlists/${id}`)
      .then(setPlaylist)
      .catch((err) => setError(err instanceof Error ? err.message : '재생목록을 불러올 수 없습니다.'));
  }, [id]);

  if (error) return <p className="text-center text-red-500 py-16">{error}</p>;
  if (!playlist) return <p className="text-center text-gray-400 py-16">로딩 중...</p>;

  const currentVideo = playlist.videos[currentIndex]?.video ?? null;
  const hasNext = currentIndex < playlist.videos.length - 1;
  const hasPrev = currentIndex > 0;

  const handleEnded = () => {
    if (hasNext) setCurrentIndex(currentIndex + 1);
  };

  return (
    <div className="flex gap-6">
      {/* 플레이어 영역 */}
      <div className="flex-1">
        <VideoPlayer hlsUrl={currentVideo?.hlsUrl ?? null} onEnded={handleEnded} />
        {currentVideo && (
          <div className="mt-4">
            <h1 className="text-xl font-bold">{currentVideo.title}</h1>
            <p className="text-sm text-gray-500 mt-1">{currentVideo.description}</p>
          </div>
        )}
        <div className="flex gap-2 mt-4">
          <button onClick={() => setCurrentIndex(currentIndex - 1)} disabled={!hasPrev}
            className="px-4 py-2 border rounded disabled:opacity-30">이전</button>
          <button onClick={() => setCurrentIndex(currentIndex + 1)} disabled={!hasNext}
            className="px-4 py-2 border rounded disabled:opacity-30">다음</button>
        </div>
      </div>

      {/* 사이드바 영상 목록 */}
      <aside className="w-72 shrink-0">
        <h3 className="font-semibold mb-3">{playlist.name}</h3>
        <div className="space-y-1 max-h-[70vh] overflow-y-auto">
          {playlist.videos.map((item, idx) => (
            <button key={item.video.id}
              onClick={() => setCurrentIndex(idx)}
              className={`w-full text-left px-3 py-2 rounded text-sm transition-colors ${
                idx === currentIndex ? 'bg-blue-50 text-blue-700' : 'hover:bg-gray-100'
              }`}>
              <div className="flex gap-2">
                <span className="text-gray-400 w-5 shrink-0">{idx + 1}</span>
                <div className="min-w-0">
                  <p className="font-medium truncate">{item.video.title}</p>
                  <p className="text-xs text-gray-400">{formatDuration(item.video.durationSeconds)}</p>
                </div>
              </div>
            </button>
          ))}
        </div>
      </aside>
    </div>
  );
}
