'use client';

import { useParams, useRouter } from 'next/navigation';
import { useEffect, useState } from 'react';
import { apiGet, apiPut, apiPost, apiDelete } from '@/lib/api';
import type { PlaylistDetailResponse, VideoResponse, PageResponse } from '@/types';

export default function PlaylistDetailPage() {
  const { id } = useParams<{ id: string }>();
  const router = useRouter();
  const [playlist, setPlaylist] = useState<PlaylistDetailResponse | null>(null);
  const [editing, setEditing] = useState(false);
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isPublic, setIsPublic] = useState(false);

  // 영상 추가 모달
  const [showAddVideo, setShowAddVideo] = useState(false);
  const [availableVideos, setAvailableVideos] = useState<VideoResponse[]>([]);

  const load = () => {
    apiGet<PlaylistDetailResponse>(`/api/v1/admin/playlists/${id}`).then((data) => {
      setPlaylist(data);
      setName(data.name);
      setDescription(data.description ?? '');
      setIsPublic(data.isPublic);
    });
  };

  useEffect(() => { load(); }, [id]);

  const handleSave = async () => {
    await apiPut(`/api/v1/admin/playlists/${id}`, { name, description, isPublic });
    setEditing(false);
    load();
  };

  const handleDelete = async () => {
    if (!confirm('재생목록을 삭제하시겠습니까?')) return;
    await apiDelete(`/api/v1/admin/playlists/${id}`);
    router.push('/admin/playlists');
  };

  const handleAddVideo = async (videoId: number) => {
    await apiPost(`/api/v1/admin/playlists/${id}/videos`, { videoId });
    setShowAddVideo(false);
    load();
  };

  const handleRemoveVideo = async (videoId: number) => {
    await apiDelete(`/api/v1/admin/playlists/${id}/videos/${videoId}`);
    load();
  };

  const handleMoveUp = async (index: number) => {
    if (!playlist || index === 0) return;
    const ids = playlist.videos.map((v) => v.video.id);
    [ids[index - 1], ids[index]] = [ids[index], ids[index - 1]];
    await apiPut(`/api/v1/admin/playlists/${id}/videos/reorder`, { videoIds: ids });
    load();
  };

  const handleMoveDown = async (index: number) => {
    if (!playlist || index >= playlist.videos.length - 1) return;
    const ids = playlist.videos.map((v) => v.video.id);
    [ids[index], ids[index + 1]] = [ids[index + 1], ids[index]];
    await apiPut(`/api/v1/admin/playlists/${id}/videos/reorder`, { videoIds: ids });
    load();
  };

  const openAddVideo = async () => {
    const data = await apiGet<PageResponse<VideoResponse>>('/api/v1/admin/videos?size=100&status=READY');
    setAvailableVideos(data.content);
    setShowAddVideo(true);
  };

  if (!playlist) return <div>로딩 중...</div>;

  return (
    <div className="max-w-3xl">
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">재생목록 상세</h2>
        <div className="flex gap-2">
          <button onClick={() => setEditing(!editing)}
            className="px-3 py-1 border rounded hover:bg-gray-100">
            {editing ? '취소' : '수정'}
          </button>
          <button onClick={handleDelete}
            className="px-3 py-1 border border-red-300 text-red-600 rounded hover:bg-red-50">
            삭제
          </button>
        </div>
      </div>

      {/* 메타정보 */}
      <div className="bg-white rounded shadow p-6 mb-6">
        {editing ? (
          <div className="space-y-3">
            <input type="text" value={name} onChange={(e) => setName(e.target.value)}
              className="w-full border rounded px-3 py-2" />
            <textarea value={description} onChange={(e) => setDescription(e.target.value)}
              className="w-full border rounded px-3 py-2" rows={2} />
            <label className="flex items-center gap-2">
              <input type="checkbox" checked={isPublic} onChange={(e) => setIsPublic(e.target.checked)} />
              <span className="text-sm">공개</span>
            </label>
            <button onClick={handleSave}
              className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">저장</button>
          </div>
        ) : (
          <div>
            <h3 className="text-xl font-semibold">{playlist.name}</h3>
            <p className="text-gray-600 mt-1">{playlist.description}</p>
            <div className="flex gap-4 mt-2 text-sm text-gray-500">
              <span>{playlist.videoCount}개 영상</span>
              <span>{Math.floor(playlist.totalDurationSeconds / 60)}분</span>
              <span>{playlist.isPublic ? '공개' : '비공개'}</span>
            </div>
          </div>
        )}
      </div>

      {/* 영상 목록 */}
      <div className="flex items-center justify-between mb-3">
        <h3 className="text-lg font-semibold">영상 목록</h3>
        <button onClick={openAddVideo}
          className="px-3 py-1 bg-green-600 text-white rounded text-sm hover:bg-green-700">
          영상 추가
        </button>
      </div>

      <div className="bg-white rounded shadow divide-y">
        {playlist.videos.map((item, idx) => (
          <div key={item.video.id} className="flex items-center px-4 py-3 gap-3">
            <span className="text-gray-400 w-8 text-center">{idx + 1}</span>
            <div className="flex-1">
              <p className="font-medium">{item.video.title}</p>
              <p className="text-sm text-gray-500">
                {item.video.durationSeconds ? `${Math.floor(item.video.durationSeconds / 60)}분 ${item.video.durationSeconds % 60}초` : '-'}
              </p>
            </div>
            <div className="flex gap-1">
              <button onClick={() => handleMoveUp(idx)} disabled={idx === 0}
                className="px-2 py-1 border rounded text-xs disabled:opacity-30">▲</button>
              <button onClick={() => handleMoveDown(idx)} disabled={idx >= playlist.videos.length - 1}
                className="px-2 py-1 border rounded text-xs disabled:opacity-30">▼</button>
              <button onClick={() => handleRemoveVideo(item.video.id)}
                className="px-2 py-1 border border-red-300 text-red-600 rounded text-xs hover:bg-red-50">제거</button>
            </div>
          </div>
        ))}
        {playlist.videos.length === 0 && (
          <p className="px-4 py-8 text-center text-gray-400">영상이 없습니다.</p>
        )}
      </div>

      {/* 영상 추가 모달 */}
      {showAddVideo && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50"
          onClick={() => setShowAddVideo(false)}>
          <div className="bg-white rounded shadow-lg w-[500px] max-h-[70vh] overflow-y-auto p-6"
            onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-semibold mb-4">영상 추가</h3>
            <div className="divide-y">
              {availableVideos.map((video) => (
                <div key={video.id} className="flex items-center justify-between py-2">
                  <span>{video.title}</span>
                  <button onClick={() => handleAddVideo(video.id)}
                    className="px-3 py-1 bg-blue-600 text-white rounded text-xs hover:bg-blue-700">
                    추가
                  </button>
                </div>
              ))}
              {availableVideos.length === 0 && (
                <p className="py-4 text-center text-gray-400">추가 가능한 영상이 없습니다.</p>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
