'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';
import { formatDuration } from '@/lib/format';
import type { VideoResponse, PageResponse } from '@/types';

export default function VideosPage() {
  const [videos, setVideos] = useState<VideoResponse[]>([]);
  const [keyword, setKeyword] = useState('');
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  const [searchTrigger, setSearchTrigger] = useState(0);

  useEffect(() => {
    const params = new URLSearchParams({ page: String(page), size: '12' });
    if (keyword) params.set('keyword', keyword);
    apiGet<PageResponse<VideoResponse>>(`/api/v1/videos?${params}`)
      .then((data) => {
        setVideos(data.content);
        setTotalPages(data.totalPages);
      })
      .catch(console.error);
  }, [page, searchTrigger]);

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    setSearchTrigger((n) => n + 1);
  };

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">영상</h2>
        <form onSubmit={handleSearch} className="flex gap-2">
          <input type="text" value={keyword} onChange={(e) => setKeyword(e.target.value)}
            placeholder="검색..." className="border rounded px-3 py-1.5 text-sm" />
          <button type="submit" className="px-3 py-1.5 bg-blue-600 text-white rounded text-sm">검색</button>
        </form>
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-4">
        {videos.map((video) => (
          <Link key={video.id} href={`/videos/${video.id}`}
            className="bg-white rounded shadow overflow-hidden hover:shadow-md transition-shadow">
            <div className="aspect-video bg-gray-200 flex items-center justify-center text-gray-400 text-sm">
              {video.thumbnailUrl
                ? <img src={video.thumbnailUrl} alt={video.title} className="w-full h-full object-cover" />
                : '썸네일 없음'}
            </div>
            <div className="p-3">
              <h3 className="font-medium text-sm line-clamp-2">{video.title}</h3>
              <div className="flex justify-between mt-1 text-xs text-gray-500">
                <span>{formatDuration(video.durationSeconds)}</span>
                <span>조회수 {video.viewCount}</span>
              </div>
            </div>
          </Link>
        ))}
      </div>

      {videos.length === 0 && (
        <p className="text-center text-gray-400 py-16">영상이 없습니다.</p>
      )}

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-6">
          <button onClick={() => setPage(Math.max(0, page - 1))} disabled={page === 0}
            className="px-3 py-1 border rounded disabled:opacity-50">이전</button>
          <span className="px-3 py-1">{page + 1} / {totalPages}</span>
          <button onClick={() => setPage(Math.min(totalPages - 1, page + 1))} disabled={page >= totalPages - 1}
            className="px-3 py-1 border rounded disabled:opacity-50">다음</button>
        </div>
      )}
    </div>
  );
}
