'use client';

import { useRouter } from 'next/navigation';
import { useState, useRef } from 'react';
import { apiUpload } from '@/lib/api';
import type { VideoResponse } from '@/types';

export default function UploadPage() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [tags, setTags] = useState('');
  const [uploading, setUploading] = useState(false);
  const [progress, setProgress] = useState(0);
  const [dragOver, setDragOver] = useState(false);

  const handleFile = (f: File) => {
    setFile(f);
    if (!title) setTitle(f.name.replace(/\.[^.]+$/, ''));
  };

  const handleDrop = (e: React.DragEvent) => {
    e.preventDefault();
    setDragOver(false);
    const f = e.dataTransfer.files[0];
    if (f) handleFile(f);
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!file || !title) return;

    setUploading(true);
    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    if (description) formData.append('description', description);
    if (tags) formData.append('tags', tags);

    try {
      await apiUpload<VideoResponse>('/api/v1/admin/videos', formData);
      router.push('/admin/videos');
    } catch (err) {
      console.error(err);
      alert('업로드 실패');
    } finally {
      setUploading(false);
    }
  };

  return (
    <div className="max-w-2xl">
      <h2 className="text-2xl font-bold mb-6">동영상 업로드</h2>

      <form onSubmit={handleSubmit} className="space-y-6">
        <div
          className={`border-2 border-dashed rounded-lg p-8 text-center cursor-pointer
            ${dragOver ? 'border-blue-500 bg-blue-50' : 'border-gray-300 hover:border-gray-400'}`}
          onDragOver={(e) => { e.preventDefault(); setDragOver(true); }}
          onDragLeave={() => setDragOver(false)}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
        >
          <input
            ref={fileInputRef}
            type="file"
            accept="video/*"
            className="hidden"
            onChange={(e) => e.target.files?.[0] && handleFile(e.target.files[0])}
          />
          {file ? (
            <p className="text-gray-700">{file.name} ({(file.size / 1024 / 1024).toFixed(1)} MB)</p>
          ) : (
            <p className="text-gray-400">파일을 드래그하거나 클릭하여 선택</p>
          )}
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">제목 *</label>
          <input type="text" value={title} onChange={(e) => setTitle(e.target.value)}
            className="w-full border rounded px-3 py-2" required />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">설명</label>
          <textarea value={description} onChange={(e) => setDescription(e.target.value)}
            className="w-full border rounded px-3 py-2" rows={3} />
        </div>

        <div>
          <label className="block text-sm font-medium mb-1">태그 (쉼표 구분)</label>
          <input type="text" value={tags} onChange={(e) => setTags(e.target.value)}
            className="w-full border rounded px-3 py-2" placeholder="태그1, 태그2" />
        </div>

        <button type="submit" disabled={!file || !title || uploading}
          className="px-6 py-2 bg-blue-600 text-white rounded hover:bg-blue-700 disabled:opacity-50">
          {uploading ? '업로드 중...' : '업로드'}
        </button>
      </form>
    </div>
  );
}
