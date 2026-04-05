'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { apiPost } from '@/lib/api';

export default function RegisterPage() {
  const router = useRouter();
  const [form, setForm] = useState({ email: '', password: '', name: '', nickname: '' });
  const [error, setError] = useState('');

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    try {
      await apiPost('/api/v1/auth/register', form);
      router.push('/login');
    } catch (err) {
      setError(err instanceof Error ? err.message : '회원가입 실패');
    }
  };

  const update = (field: keyof typeof form, value: string) => setForm({ ...form, [field]: value });

  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <form onSubmit={handleSubmit} className="bg-white p-8 rounded shadow w-96 space-y-4">
        <h1 className="text-2xl font-bold text-center">회원가입</h1>
        {error && <p className="text-red-500 text-sm">{error}</p>}
        <div>
          <label className="block text-sm font-medium mb-1">이메일</label>
          <input type="email" value={form.email} onChange={(e) => update('email', e.target.value)}
            className="w-full border rounded px-3 py-2" required />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">비밀번호 (8자 이상)</label>
          <input type="password" value={form.password} onChange={(e) => update('password', e.target.value)}
            className="w-full border rounded px-3 py-2" required minLength={8} />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">이름</label>
          <input type="text" value={form.name} onChange={(e) => update('name', e.target.value)}
            className="w-full border rounded px-3 py-2" required />
        </div>
        <div>
          <label className="block text-sm font-medium mb-1">닉네임</label>
          <input type="text" value={form.nickname} onChange={(e) => update('nickname', e.target.value)}
            className="w-full border rounded px-3 py-2" required />
        </div>
        <button type="submit" className="w-full py-2 bg-blue-600 text-white rounded hover:bg-blue-700">
          가입하기
        </button>
        <p className="text-center text-sm text-gray-500">
          이미 계정이 있나요? <Link href="/login" className="text-blue-600 hover:underline">로그인</Link>
        </p>
      </form>
    </div>
  );
}
