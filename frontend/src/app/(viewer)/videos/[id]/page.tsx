'use client';

import { useParams } from 'next/navigation';
import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';
import { formatDuration, formatDate } from '@/lib/format';
import VideoPlayer from '@/components/video/VideoPlayer';
import type { VideoResponse } from '@/types';

export default function VideoPlayPage() {
  const { id } = useParams<{ id: string }>();
  const [video, setVideo] = useState<VideoResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiGet<VideoResponse>(`/api/v1/videos/${id}`)
      .then(setVideo)
      .catch((err) => setError(err instanceof Error ? err.message : '영상을 불러올 수 없습니다.'));
  }, [id]);

  if (error) return <p className="text-center text-red-500 py-16">{error}</p>;
  if (!video) return <p className="text-center text-gray-400 py-16">로딩 중...</p>;

  return (
    <div className="max-w-4xl mx-auto">
      <VideoPlayer hlsUrl={video.hlsUrl} />

      <div className="mt-4">
        <h1 className="text-xl font-bold">{video.title}</h1>
        <div className="flex gap-4 mt-2 text-sm text-gray-500">
          <span>조회수 {video.viewCount}</span>
          <span>{formatDuration(video.durationSeconds)}</span>
          <span>{formatDate(video.createdAt)}</span>
        </div>
        {video.description && (
          <p className="mt-4 text-gray-700 whitespace-pre-wrap">{video.description}</p>
        )}
        {video.tags && (
          <div className="mt-3 flex gap-2">
            {video.tags.split(',').map((tag) => (
              <span key={tag.trim()} className="px-2 py-0.5 bg-gray-100 rounded text-xs text-gray-600">
                {tag.trim()}
              </span>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}
