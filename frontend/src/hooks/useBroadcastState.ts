'use client';

import { useCallback, useEffect, useRef, useState } from 'react';
import type { BroadcastStateResponse } from '@/types';

const WS_URL = process.env.NEXT_PUBLIC_WS_URL || '';
const RECONNECT_DELAY = 3000;

export function useBroadcastState() {
  const [state, setState] = useState<BroadcastStateResponse | null>(null);
  const [connected, setConnected] = useState(false);
  const wsRef = useRef<WebSocket | null>(null);
  const reconnectTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const mountedRef = useRef(true);

  const connect = useCallback(() => {
    if (!mountedRef.current) return;

    let url: string;
    if (WS_URL) {
      url = WS_URL;
    } else {
      const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
      url = `${protocol}//${window.location.hostname}:8080/ws/broadcast`;
    }

    const ws = new WebSocket(url);
    wsRef.current = ws;

    ws.onopen = () => setConnected(true);

    ws.onclose = (event) => {
      setConnected(false);
      // 정상 종료가 아니면 재연결
      if (event.code !== 1000 && mountedRef.current) {
        reconnectTimer.current = setTimeout(connect, RECONNECT_DELAY);
      }
    };

    ws.onerror = () => {
      // onclose가 이어서 호출되므로 별도 처리 불필요
    };

    ws.onmessage = (event) => {
      try {
        const data: BroadcastStateResponse = JSON.parse(event.data);
        setState(data);
      } catch {
        // 잘못된 메시지 무시
      }
    };
  }, []);

  useEffect(() => {
    mountedRef.current = true;
    connect();

    return () => {
      mountedRef.current = false;
      if (reconnectTimer.current) {
        clearTimeout(reconnectTimer.current);
      }
      if (wsRef.current) {
        wsRef.current.close(1000);
        wsRef.current = null;
      }
    };
  }, [connect]);

  return { state, connected };
}
