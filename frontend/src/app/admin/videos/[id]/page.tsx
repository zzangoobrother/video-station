'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { apiGet, apiPut, apiDelete, apiPatch } from '@/lib/api';
import type { VideoResponse } from '@/types';

export default function VideoDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [video, setVideo] = useState<VideoResponse | null>(null);
  const [editing, setEditing] = useState(false);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState('');

  useEffect(() => {
    apiGet<VideoResponse>(`/api/v1/admin/videos/${id}`).then((data) => {
      setVideo(data);
      setTitle(data.title);
      setDescription(data.description ?? '');
      setTags(data.tags ?? '');
    });
  }, [id]);

  // 인코딩 중이면 폴링
  useEffect(() => {
    if (!video || !['UPLOADING', 'ENCODING_QUEUED', 'ENCODING'].includes(video.status)) return;
    const interval = setInterval(() => {
      apiGet<VideoResponse>(`/api/v1/admin/videos/${id}/encoding-status`).then(setVideo);
    }, 3000);
    return () => clearInterval(interval);
  }, [id, video?.status]);

  if (!video) return <div>로딩 중...</div>;

  const handleSave = async () => {
    const updated = await apiPut<VideoResponse>(`/api/v1/admin/videos/${id}`, { title, description, tags });
    setVideo(updated);
    setEditing(false);
  };

  const handleDelete = async () => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    await apiDelete(`/api/v1/admin/videos/${id}`);
    router.push('/admin/videos');
  };

  const handleToggle = async () => {
    const updated = await apiPatch<VideoResponse>(`/api/v1/admin/videos/${id}/visibility`);
    setVideo(updated);
  };

  return (
    <div className="max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">동영상 상세</h2>
        <div className="flex gap-2">
          <button onClick={() => setEditing(!editing)}
            className="px-3 py-1 border rounded hover:bg-gray-100">
            {editing ? '취소' : '수정'}
          </button>
          <button onClick={handleToggle}
            className="px-3 py-1 border rounded hover:bg-gray-100">
            {video.isPublic ? '비공개로' : '공개로'}
          </button>
          <button onClick={handleDelete}
            className="px-3 py-1 border border-red-300 text-red-600 rounded hover:bg-red-50">
            삭제
          </button>
        </div>
      </div>

      <div className="bg-white rounded shadow p-6 space-y-4">
        {editing ? (
          <>
            <div>
              <label className="block text-sm font-medium mb-1">제목</label>
              <input type="text" value={title} onChange={(e) => setTitle(e.target.value)}
                className="w-full border rounded px-3 py-2" />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">설명</label>
              <textarea value={description} onChange={(e) => setDescription(e.target.value)}
                className="w-full border rounded px-3 py-2" rows={3} />
            </div>
            <div>
              <label className="block text-sm font-medium mb-1">태그</label>
              <input type="text" value={tags} onChange={(e) => setTags(e.target.value)}
                className="w-full border rounded px-3 py-2" />
            </div>
            <button onClick={handleSave}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">저장</button>
          </>
        ) : (
          <>
            <h3 className="text-xl font-semibold">{video.title}</h3>
            <p className="text-gray-600">{video.description}</p>
            <div className="grid grid-cols-2 gap-4 text-sm">
              <div><span className="text-gray-500">파일명:</span> {video.originalFileName}</div>
              <div><span className="text-gray-500">크기:</span> {video.fileSize ? `${(video.fileSize / 1024 / 1024).toFixed(1)} MB` : '-'}</div>
              <div><span className="text-gray-500">상태:</span> {video.status}</div>
              <div><span className="text-gray-500">공개:</span> {video.isPublic ? '공개' : '비공개'}</div>
              <div><span className="text-gray-500">조회수:</span> {video.viewCount}</div>
              <div><span className="text-gray-500">재생시간:</span> {video.durationSeconds ? `${Math.floor(video.durationSeconds / 60)}분 ${video.durationSeconds % 60}초` : '-'}</div>
              <div><span className="text-gray-500">태그:</span> {video.tags || '-'}</div>
              <div><span className="text-gray-500">등록일:</span> {new Date(video.createdAt).toLocaleString('ko-KR')}</div>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
