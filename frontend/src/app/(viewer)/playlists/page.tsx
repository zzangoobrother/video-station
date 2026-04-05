'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';
import { formatDuration } from '@/lib/format';
import type { PlaylistResponse, PageResponse } from '@/types';

export default function PlaylistsPage() {
  const [playlists, setPlaylists] = useState<PlaylistResponse[]>([]);

  useEffect(() => {
    apiGet<PageResponse<PlaylistResponse>>('/api/v1/playlists?size=50')
      .then((data) => setPlaylists(data.content))
      .catch(console.error);
  }, []);

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">재생목록</h2>

      <div className="grid grid-cols-1 sm:grid-cols-2 md:grid-cols-3 gap-4">
        {playlists.map((pl) => (
          <Link key={pl.id} href={`/playlists/${pl.id}`}
            className="bg-white rounded shadow p-4 hover:shadow-md transition-shadow">
            <h3 className="font-medium">{pl.name}</h3>
            <p className="text-sm text-gray-500 mt-1 line-clamp-2">{pl.description}</p>
            <div className="flex gap-3 mt-2 text-xs text-gray-400">
              <span>{pl.videoCount}개 영상</span>
              <span>{formatDuration(pl.totalDurationSeconds)}</span>
            </div>
          </Link>
        ))}
      </div>

      {playlists.length === 0 && (
        <p className="text-center text-gray-400 py-16">재생목록이 없습니다.</p>
      )}
    </div>
  );
}
