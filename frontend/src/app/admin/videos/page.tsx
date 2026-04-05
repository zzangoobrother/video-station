'use client';

import Link from 'next/link';
import { useEffect, useState } from 'react';
import { apiGet } from '@/lib/api';
import type { VideoResponse, PageResponse } from '@/types';

const STATUS_LABELS: Record<string, { label: string; color: string }> = {
  UPLOADING: { label: '업로드 중', color: 'bg-blue-100 text-blue-800' },
  ENCODING_QUEUED: { label: '인코딩 대기', color: 'bg-yellow-100 text-yellow-800' },
  ENCODING: { label: '인코딩 중', color: 'bg-orange-100 text-orange-800' },
  READY: { label: '완료', color: 'bg-green-100 text-green-800' },
  FAILED: { label: '실패', color: 'bg-red-100 text-red-800' },
  DELETED: { label: '삭제됨', color: 'bg-gray-100 text-gray-800' },
};

export default function AdminVideosPage() {
  const [videos, setVideos] = useState<VideoResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);

  useEffect(() => {
    apiGet<PageResponse<VideoResponse>>(`/api/v1/admin/videos?page=${page}&size=20`)
      .then((data) => {
        setVideos(data.content);
        setTotalPages(data.totalPages);
      })
      .catch(console.error);
  }, [page]);

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">동영상 관리</h2>
        <Link
          href="/admin/videos/upload"
          className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
        >
          업로드
        </Link>
      </div>

      <div className="bg-white rounded shadow overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-4 py-3">제목</th>
              <th className="px-4 py-3">파일명</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3">공개</th>
              <th className="px-4 py-3">조회수</th>
              <th className="px-4 py-3">등록일</th>
            </tr>
          </thead>
          <tbody>
            {videos.map((video) => {
              const status = STATUS_LABELS[video.status] ?? { label: video.status, color: '' };
              return (
                <tr key={video.id} className="border-t hover:bg-gray-50">
                  <td className="px-4 py-3">
                    <Link href={`/admin/videos/${video.id}`} className="text-blue-600 hover:underline">
                      {video.title}
                    </Link>
                  </td>
                  <td className="px-4 py-3 text-sm text-gray-500">{video.originalFileName}</td>
                  <td className="px-4 py-3">
                    <span className={`px-2 py-1 rounded text-xs font-medium ${status.color}`}>
                      {status.label}
                    </span>
                  </td>
                  <td className="px-4 py-3">{video.isPublic ? '공개' : '비공개'}</td>
                  <td className="px-4 py-3">{video.viewCount}</td>
                  <td className="px-4 py-3 text-sm text-gray-500">
                    {new Date(video.createdAt).toLocaleDateString('ko-KR')}
                  </td>
                </tr>
              );
            })}
            {videos.length === 0 && (
              <tr>
                <td colSpan={6} className="px-4 py-8 text-center text-gray-400">
                  등록된 동영상이 없습니다.
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="flex justify-center gap-2 mt-4">
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
