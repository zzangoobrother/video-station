'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';
import { formatDuration, formatDate } from '@/lib/format';
import type { PlaylistResponse, PageResponse } from '@/types';

export default function AdminPlaylistsPage() {
  const [playlists, setPlaylists] = useState<PlaylistResponse[]>([]);

  useEffect(() => {
    apiGet<PageResponse<PlaylistResponse>>('/api/v1/admin/playlists?size=50')
      .then((data) => setPlaylists(data.content))
      .catch(console.error);
  }, []);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">재생목록 관리</h2>
        <Link href="/admin/playlists/new"
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          새 재생목록
        </Link>
      </div>

      <div className="bg-white rounded shadow overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-4 py-3">이름</th>
              <th className="px-4 py-3">영상 수</th>
              <th className="px-4 py-3">총 재생시간</th>
              <th className="px-4 py-3">공개</th>
              <th className="px-4 py-3">등록일</th>
            </tr>
          </thead>
          <tbody>
            {playlists.map((pl) => (
              <tr key={pl.id} className="border-t hover:bg-gray-50">
                <td className="px-4 py-3">
                  <Link href={`/admin/playlists/${pl.id}`} className="text-blue-600 hover:underline">
                    {pl.name}
                  </Link>
                </td>
                <td className="px-4 py-3">{pl.videoCount}개</td>
                <td className="px-4 py-3">{formatDuration(pl.totalDurationSeconds)}</td>
                <td className="px-4 py-3">{pl.isPublic ? '공개' : '비공개'}</td>
                <td className="px-4 py-3 text-sm text-gray-500">
                  {formatDate(pl.createdAt)}
                </td>
              </tr>
            ))}
            {playlists.length === 0 && (
              <tr>
                <td colSpan={5} className="px-4 py-8 text-center text-gray-400">
                  재생목록이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  );
}
