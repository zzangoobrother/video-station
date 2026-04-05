'use client';

import { useEffect, useState } from 'react';
import { apiGet, apiPost } from '@/lib/api';
import { formatDuration } from '@/lib/format';
import type {
  ChannelResponse,
  PageResponse,
  PlaylistResponse,
  BroadcastStateResponse,
} from '@/types';
import { useAuth } from '@/hooks/useAuth';

const STATUS_STYLE: Record<string, { label: string; color: string }> = {
  IDLE: { label: '대기', color: 'bg-gray-200 text-gray-700' },
  LIVE: { label: 'LIVE', color: 'bg-red-500 text-white' },
  PAUSED: { label: '일시정지', color: 'bg-yellow-400 text-gray-900' },
  ENDED: { label: '종료', color: 'bg-gray-400 text-white' },
  SCHEDULED: { label: '예약됨', color: 'bg-blue-400 text-white' },
};

export default function BroadcastPage() {
  const { user } = useAuth();
  const [channels, setChannels] = useState<ChannelResponse[]>([]);
  const [playlists, setPlaylists] = useState<PlaylistResponse[]>([]);
  const [selectedChannel, setSelectedChannel] = useState<ChannelResponse | null>(null);
  const [broadcastState, setBroadcastState] = useState<BroadcastStateResponse | null>(null);

  // 방송 시작 폼
  const [playlistId, setPlaylistId] = useState<number | ''>('');
  const [loopPlaylist, setLoopPlaylist] = useState(false);

  // 채널 생성 폼
  const [showCreateChannel, setShowCreateChannel] = useState(false);
  const [newChannelName, setNewChannelName] = useState('');
  const [newChannelDesc, setNewChannelDesc] = useState('');

  useEffect(() => {
    loadChannels();
    apiGet<PageResponse<PlaylistResponse>>('/api/v1/admin/playlists?size=100')
      .then((data) => setPlaylists(data.content))
      .catch(console.error);
  }, []);

  const loadChannels = () => {
    apiGet<PageResponse<ChannelResponse>>('/api/v1/channels?size=50')
      .then((data) => setChannels(data.content))
      .catch(console.error);
  };

  const selectChannel = async (channel: ChannelResponse) => {
    setSelectedChannel(channel);
    setBroadcastState(null);
    if (channel.status !== 'IDLE' && channel.status !== 'SCHEDULED') {
      try {
        const state = await apiGet<BroadcastStateResponse>(
          `/api/v1/channels/${channel.id}/broadcast/state`,
        );
        setBroadcastState(state);
      } catch {
        setBroadcastState(null);
      }
    } else {
      setBroadcastState(null);
    }
  };

  const handleCreateChannel = async () => {
    if (!newChannelName.trim()) return;
    try {
      await apiPost('/api/v1/channels', { name: newChannelName, description: newChannelDesc });
      setShowCreateChannel(false);
      setNewChannelName('');
      setNewChannelDesc('');
      loadChannels();
    } catch {
      alert('채널 생성에 실패했습니다.');
    }
  };

  const handleStart = async () => {
    if (!selectedChannel || !playlistId) return;
    try {
      const state = await apiPost<BroadcastStateResponse>(
        `/api/v1/channels/${selectedChannel.id}/broadcast/start`,
        { playlistId, loopPlaylist },
      );
      setBroadcastState(state);
      loadChannels();
    } catch {
      alert('방송 시작에 실패했습니다.');
    }
  };

  type BroadcastAction = 'pause' | 'resume' | 'stop' | 'next' | 'previous';

  const handleAction = async (action: BroadcastAction) => {
    if (!selectedChannel) return;
    if (action === 'stop') {
      await apiPost(`/api/v1/channels/${selectedChannel.id}/broadcast/stop`);
      setBroadcastState(null);
      loadChannels();
      return;
    }
    try {
      const state = await apiPost<BroadcastStateResponse>(
        `/api/v1/channels/${selectedChannel.id}/broadcast/${action}`,
      );
      setBroadcastState(state);
    } catch {
      alert('작업에 실패했습니다.');
    }
    loadChannels();
  };

  const handleJump = async (index: number) => {
    if (!selectedChannel) return;
    try {
      const state = await apiPost<BroadcastStateResponse>(
        `/api/v1/channels/${selectedChannel.id}/broadcast/jump/${index}`,
      );
      setBroadcastState(state);
    } catch {
      alert('영상 이동에 실패했습니다.');
    }
  };

  const isLiveOrPaused = broadcastState && (broadcastState.status === 'LIVE' || broadcastState.status === 'PAUSED');
  const statusInfo = broadcastState ? STATUS_STYLE[broadcastState.status] : null;

  return (
    <div>
      <div className="flex items-center justify-between mb-6">
        <h2 className="text-2xl font-bold">방송 제어</h2>
        {user?.role === 'SUPER_ADMIN' && (
          <button
            onClick={() => setShowCreateChannel(true)}
            className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700"
          >
            채널 생성
          </button>
        )}
      </div>

      <div className="flex gap-6">
        {/* 채널 목록 */}
        <div className="w-60 shrink-0">
          <h3 className="font-semibold mb-3">채널 목록</h3>
          <div className="space-y-1">
            {channels.map((ch) => (
              <button
                key={ch.id}
                onClick={() => selectChannel(ch)}
                className={`w-full text-left px-3 py-2 rounded flex items-center justify-between ${
                  selectedChannel?.id === ch.id ? 'bg-blue-50 text-blue-700' : 'hover:bg-gray-100'
                }`}
              >
                <span className="truncate">{ch.name}</span>
                <span className={`ml-2 px-1.5 py-0.5 rounded text-xs font-medium shrink-0 ${STATUS_STYLE[ch.status]?.color ?? ''}`}>
                  {STATUS_STYLE[ch.status]?.label ?? ch.status}
                </span>
              </button>
            ))}
            {channels.length === 0 && (
              <p className="text-sm text-gray-400 px-3">채널이 없습니다.</p>
            )}
          </div>
        </div>

        {/* 방송 제어 패널 */}
        <div className="flex-1">
          {!selectedChannel ? (
            <div className="bg-white rounded shadow p-8 text-center text-gray-400">
              채널을 선택하세요.
            </div>
          ) : isLiveOrPaused && broadcastState ? (
            /* 방송 진행 중 */
            <div className="bg-white rounded shadow p-6 space-y-6">
              <div className="flex items-center gap-3">
                <h3 className="text-xl font-bold">{selectedChannel.name}</h3>
                {statusInfo && (
                  <span className={`px-2 py-1 rounded text-xs font-bold ${statusInfo.color}`}>
                    {statusInfo.label}
                  </span>
                )}
              </div>

              {/* 현재 영상 정보 */}
              <div className="bg-gray-50 rounded p-4">
                <p className="text-sm text-gray-500 mb-1">현재 재생 중</p>
                <p className="font-semibold text-lg">{broadcastState.currentVideo?.title}</p>
                <p className="text-sm text-gray-500 mt-1">
                  재생목록: {broadcastState.playlistName} ({broadcastState.currentVideoIndex + 1} / {broadcastState.totalVideosInPlaylist})
                </p>
                <p className="text-sm text-gray-500">
                  재생 위치: {formatDuration(broadcastState.offsetSeconds)} / {formatDuration(broadcastState.currentVideo?.durationSeconds)}
                </p>
                {broadcastState.nextVideo && (
                  <p className="text-sm text-gray-400 mt-2">
                    다음 영상: {broadcastState.nextVideo.title}
                  </p>
                )}
              </div>

              {/* 제어 버튼 */}
              <div className="flex gap-3">
                <button
                  onClick={() => handleAction('previous')}
                  className="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300"
                >
                  이전
                </button>
                {broadcastState.status === 'LIVE' ? (
                  <button
                    onClick={() => handleAction('pause')}
                    className="px-4 py-2 bg-yellow-400 rounded hover:bg-yellow-500"
                  >
                    일시정지
                  </button>
                ) : (
                  <button
                    onClick={() => handleAction('resume')}
                    className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
                  >
                    재개
                  </button>
                )}
                <button
                  onClick={() => handleAction('next')}
                  className="px-4 py-2 bg-gray-200 rounded hover:bg-gray-300"
                >
                  다음
                </button>
                <button
                  onClick={() => handleAction('stop')}
                  className="px-4 py-2 bg-red-500 text-white rounded hover:bg-red-600"
                >
                  방송 종료
                </button>
              </div>

              {/* 영상 점프 */}
              <div>
                <p className="text-sm font-medium mb-2">영상 점프</p>
                <div className="flex gap-2 flex-wrap">
                  {Array.from({ length: broadcastState.totalVideosInPlaylist }, (_, i) => (
                    <button
                      key={i}
                      onClick={() => handleJump(i)}
                      className={`w-8 h-8 rounded text-sm ${
                        i === broadcastState.currentVideoIndex
                          ? 'bg-blue-600 text-white'
                          : 'bg-gray-100 hover:bg-gray-200'
                      }`}
                    >
                      {i + 1}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          ) : (
            /* 방송 시작 폼 */
            <div className="bg-white rounded shadow p-6 space-y-4">
              <h3 className="text-xl font-bold">{selectedChannel.name} - 방송 시작</h3>
              <div>
                <label className="block text-sm font-medium mb-1">재생목록 선택</label>
                <select
                  value={playlistId}
                  onChange={(e) => setPlaylistId(e.target.value ? Number(e.target.value) : '')}
                  className="w-full border rounded px-3 py-2"
                >
                  <option value="">선택하세요</option>
                  {playlists.map((pl) => (
                    <option key={pl.id} value={pl.id}>
                      {pl.name} ({pl.videoCount}개, {formatDuration(pl.totalDurationSeconds)})
                    </option>
                  ))}
                </select>
              </div>
              <label className="flex items-center gap-2">
                <input
                  type="checkbox"
                  checked={loopPlaylist}
                  onChange={(e) => setLoopPlaylist(e.target.checked)}
                />
                <span className="text-sm">재생목록 반복</span>
              </label>
              <button
                onClick={handleStart}
                disabled={!playlistId}
                className="px-6 py-2 bg-red-500 text-white rounded hover:bg-red-600 disabled:opacity-50"
              >
                방송 시작
              </button>
            </div>
          )}
        </div>
      </div>

      {/* 채널 생성 모달 */}
      {showCreateChannel && (
        <div className="fixed inset-0 bg-black/30 flex items-center justify-center z-50" onClick={() => setShowCreateChannel(false)}>
          <div className="bg-white rounded shadow-lg w-96 p-6" onClick={(e) => e.stopPropagation()}>
            <h3 className="text-lg font-bold mb-4">채널 생성</h3>
            <div className="space-y-3">
              <div>
                <label className="block text-sm font-medium mb-1">채널명</label>
                <input
                  value={newChannelName}
                  onChange={(e) => setNewChannelName(e.target.value)}
                  className="w-full border rounded px-3 py-2"
                  placeholder="채널명"
                />
              </div>
              <div>
                <label className="block text-sm font-medium mb-1">설명</label>
                <textarea
                  value={newChannelDesc}
                  onChange={(e) => setNewChannelDesc(e.target.value)}
                  className="w-full border rounded px-3 py-2"
                  rows={3}
                  placeholder="채널 설명 (선택)"
                />
              </div>
              <div className="flex justify-end gap-2">
                <button onClick={() => setShowCreateChannel(false)} className="px-4 py-2 border rounded">취소</button>
                <button onClick={handleCreateChannel} className="px-4 py-2 bg-blue-600 text-white rounded hover:bg-blue-700">생성</button>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
