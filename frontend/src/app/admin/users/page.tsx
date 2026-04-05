'use client';

import { useEffect, useState } from 'react';
import { apiGet, apiPatch, apiDelete } from '@/lib/api';
import type { UserResponse, PageResponse } from '@/types';

const ROLES = ['VIEWER', 'ADMIN', 'SUPER_ADMIN'] as const;
const STATUSES = ['ACTIVE', 'INACTIVE', 'BANNED'] as const;

export default function AdminUsersPage() {
  const [users, setUsers] = useState<UserResponse[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [refreshKey, setRefreshKey] = useState(0);

  useEffect(() => {
    apiGet<PageResponse<UserResponse>>(`/api/v1/admin/users?page=${page}&size=20`)
      .then((data) => {
        setUsers(data.content);
        setTotalPages(data.totalPages);
      })
      .catch(console.error);
  }, [page, refreshKey]);

  const handleRoleChange = async (userId: number, role: string) => {
    try {
      await apiPatch(`/api/v1/admin/users/${userId}/role`, { role });
      setRefreshKey((n) => n + 1);
    } catch (err) {
      alert(err instanceof Error ? err.message : '역할 변경 실패');
    }
  };

  const handleStatusChange = async (userId: number, status: string) => {
    try {
      await apiPatch(`/api/v1/admin/users/${userId}/status`, { status });
      setRefreshKey((n) => n + 1);
    } catch (err) {
      alert(err instanceof Error ? err.message : '상태 변경 실패');
    }
  };

  const handleDelete = async (userId: number) => {
    if (!confirm('정말 삭제하시겠습니까?')) return;
    try {
      await apiDelete(`/api/v1/admin/users/${userId}`);
      setRefreshKey((n) => n + 1);
    } catch (err) {
      alert(err instanceof Error ? err.message : '삭제 실패');
    }
  };

  return (
    <div>
      <h2 className="text-2xl font-bold mb-6">사용자 관리</h2>

      <div className="bg-white rounded shadow overflow-hidden">
        <table className="w-full text-left">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-4 py-3">이메일</th>
              <th className="px-4 py-3">이름</th>
              <th className="px-4 py-3">닉네임</th>
              <th className="px-4 py-3">역할</th>
              <th className="px-4 py-3">상태</th>
              <th className="px-4 py-3">관리</th>
            </tr>
          </thead>
          <tbody>
            {users.map((user) => (
              <tr key={user.id} className="border-t">
                <td className="px-4 py-3">{user.email}</td>
                <td className="px-4 py-3">{user.name}</td>
                <td className="px-4 py-3">{user.nickname}</td>
                <td className="px-4 py-3">
                  <select value={user.role}
                    onChange={(e) => handleRoleChange(user.id, e.target.value)}
                    className="border rounded px-2 py-1 text-sm">
                    {ROLES.map((r) => <option key={r} value={r}>{r}</option>)}
                  </select>
                </td>
                <td className="px-4 py-3">
                  <select value={user.status}
                    onChange={(e) => handleStatusChange(user.id, e.target.value)}
                    className="border rounded px-2 py-1 text-sm">
                    {STATUSES.map((s) => <option key={s} value={s}>{s}</option>)}
                  </select>
                </td>
                <td className="px-4 py-3">
                  <button onClick={() => handleDelete(user.id)}
                    className="text-red-600 text-sm hover:underline">삭제</button>
                </td>
              </tr>
            ))}
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
