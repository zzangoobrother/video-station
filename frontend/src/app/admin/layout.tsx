'use client';

import Link from 'next/link';
import { usePathname, useRouter } from 'next/navigation';
import { useEffect } from 'react';
import { useAuth } from '@/hooks/useAuth';
import type { UserResponse } from '@/types';

const navItems: { href: string; label: string; requiredRole?: string }[] = [
  { href: '/admin/videos', label: '동영상 관리' },
  { href: '/admin/playlists', label: '재생목록 관리' },
  { href: '/admin/broadcast', label: '방송 제어' },
  { href: '/admin/users', label: '사용자 관리', requiredRole: 'SUPER_ADMIN' },
];

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  const pathname = usePathname();
  const router = useRouter();
  const { user, loading } = useAuth();

  useEffect(() => {
    if (!loading && !user) {
      router.replace('/login');
    }
  }, [loading, user, router]);

  if (loading || !user) return null;

  const visibleItems = navItems.filter(
    (item) => !item.requiredRole || item.requiredRole === user.role,
  );

  return (
    <div className="flex min-h-screen">
      <aside className="w-60 bg-gray-900 text-white p-4">
        <h1 className="text-xl font-bold mb-8">
          <Link href="/videos">Video Station</Link>
        </h1>
        <nav className="space-y-1">
          {visibleItems.map((item) => (
            <Link
              key={item.href}
              href={item.href}
              className={`block px-3 py-2 rounded ${
                pathname.startsWith(item.href) ? 'bg-gray-700' : 'hover:bg-gray-800'
              }`}
            >
              {item.label}
            </Link>
          ))}
        </nav>
      </aside>
      <main className="flex-1 bg-gray-50 p-8">{children}</main>
    </div>
  );
}
