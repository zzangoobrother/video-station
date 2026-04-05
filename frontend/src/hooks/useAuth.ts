'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import { apiGet, apiPost, setAccessToken, getAccessToken } from '@/lib/api';
import type { TokenResponse, UserResponse } from '@/types';

export function useAuth() {
  const router = useRouter();
  const [user, setUser] = useState<UserResponse | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const token = getAccessToken();
    if (!token) {
      setLoading(false);
      return;
    }

    apiGet<UserResponse>('/api/v1/auth/me')
      .then(setUser)
      .catch(() => setAccessToken(null))
      .finally(() => setLoading(false));
  }, []);

  const logout = () => {
    setAccessToken(null);
    setUser(null);
    router.push('/login');
  };

  const refresh = async () => {
    try {
      const data = await apiPost<TokenResponse>('/api/v1/auth/refresh');
      setAccessToken(data.accessToken);
    } catch {
      logout();
    }
  };

  return { user, loading, logout, refresh, isAuthenticated: !!user };
}
