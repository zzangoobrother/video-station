'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiPost } from '@/lib/api';
import type { PlaylistResponse } from '@/types';

export default function NewPlaylistPage() {
  const router = useRouter();
  const [name, setName] = useState('');
  const [description, setDescription] = useState('');
  const [isPublic, setIsPublic] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    await apiPost<PlaylistResponse>('/api/v1/admin/playlists', { name, description, isPublic });
    router.push('/admin/playlists');
  };

  return (
    <div className="max-w-xl">
      <h2 className="text-2xl font-bold mb-6">새 재생목록</h2>
      <form onSubmit={handleSubmit} className="bg-white rounded shadow p-6 space-y-4">
        <div>
          <label className="block text-sm font-medium mb-1">이름 *</label>
          <input type="text" value={name} onChange={(e) => setName(e.target.value)}
            className="w-full border rounded px-3 py-2" required />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">설명</label>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2" rows={3} />
        </div>
        <label className="flex items-center gap-2">
          <input type="checkbox" checked={isPublic} onChange={(e) => setIsPublic(e.target.checked)} />
          <span className="text-sm">공개</span>
        </label>
        <button type="submit" disabled={!name}
          className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
          생성
        </button>
      </form>
    </div>
  );
}
