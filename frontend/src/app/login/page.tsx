'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiPost, setAccessToken } from '@/lib/api';
import type { TokenResponse } from '@/types';

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState('admin@test.com');
  const [password, setPassword] = useState('admin1234');
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      const data = await apiPost<TokenResponse>('/api/v1/auth/login', { email, password });
      setAccessToken(data.accessToken);
      localStorage.setItem('accessToken', data.accessToken);
      router.push('/admin/videos');
    } catch (err: any) {
      setError(err.message || '로그인 실패');
    }
  };

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded shadow w-96 space-y-4">
        <h1 className="text-2xl font-bold text-center">Video Station</h1>
        {error && <p className="text-red-500 text-sm">{error}</p>}
        <div>
          <label className="block text-sm font-medium mb-1">이메일</label>
          <input type="email" value={email} onChange={(e) => setEmail(e.target.value)}
            className="w-full border rounded px-3 py-2" required />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">비밀번호</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)}
            className="w-full border rounded px-3 py-2" required />
        </div>
        <button type="submit" className="w-full py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          로그인
        </button>
      </form>
    </div>
  );
}
