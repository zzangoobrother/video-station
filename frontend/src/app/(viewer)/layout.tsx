'use client';

import Link from 'next/link';
import { useEffect } from 'react';
import { useRouter } from 'next/navigation';
import { useAuth } from '@/hooks/useAuth';

export default function ViewerLayout({ children }: { children: React.ReactNode }) {
  const router = useRouter();
  const { user, loading, logout } = useAuth();

  useEffect(() => {
    if (!loading && !user) {
      router.replace('/login');
    }
  }, [loading, user, router]);

  if (loading || !user) return null;

  return (
    <div className="min-h-screen bg-gray-50">
      <header className="bg-white shadow-sm">
        <div className="max-w-6xl mx-auto px-4 h-14 flex items-center justify-between">
          <Link href="/videos" className="text-lg font-bold">Video Station</Link>
          <nav className="flex items-center gap-4">
            <Link href="/live" className="text-sm hover:text-blue-600 font-semibold">라이브</Link>
            <Link href="/videos" className="text-sm hover:text-blue-600">영상</Link>
            <Link href="/playlists" className="text-sm hover:text-blue-600">재생목록</Link>
            {user.role !== 'VIEWER' && (
              <Link href="/admin/videos" className="text-sm text-gray-400 hover:text-blue-600">관리</Link>
            )}
            <span className="text-sm text-gray-500">{user.nickname}</span>
            <button onClick={logout} className="text-sm text-gray-400 hover:text-red-500">로그아웃</button>
          </nav>
        </div>
      </header>
      <main className="max-w-6xl mx-auto px-4 py-8">{children}</main>
    </div>
  );
}
