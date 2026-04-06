'use client';

import { useRouter } from 'next/navigation';
import { useRef, useState, useCallback, useEffect, type ReactNode, type RefObject } from 'react';

interface FullscreenPlayerProps {
  videoRef: RefObject<HTMLVideoElement | null>;
  title: string;
  subtitle?: string;
  /** 상단 좌측 뱃지 영역 (LIVE 등) */
  badge?: ReactNode;
  /** 하단 추가 정보 */
  bottomExtra?: ReactNode;
  /** 에러 메시지 */
  error?: string | null;
}

export default function FullscreenPlayer({
  videoRef,
  title,
  subtitle,
  badge,
  bottomExtra,
  error,
}: FullscreenPlayerProps) {
  const router = useRouter();
  const hideTimer = useRef<ReturnType<typeof setTimeout> | null>(null);
  const [showOverlay, setShowOverlay] = useState(true);

  const resetHideTimer = useCallback(() => {
    setShowOverlay(true);
    if (hideTimer.current) clearTimeout(hideTimer.current);
    hideTimer.current = setTimeout(() => setShowOverlay(false), 3000);
  }, []);

  const handleScreenTap = useCallback(() => {
    if (showOverlay) {
      setShowOverlay(false);
      if (hideTimer.current) clearTimeout(hideTimer.current);
    } else {
      resetHideTimer();
    }
  }, [showOverlay, resetHideTimer]);

  // 초기 3초 후 오버레이 숨김
  useEffect(() => {
    const timer = setTimeout(() => setShowOverlay(false), 3000);
    return () => clearTimeout(timer);
  }, []);

  const handleBack = useCallback(() => {
    if (document.fullscreenElement) {
      document.exitFullscreen().then(() => router.back());
    } else {
      router.back();
    }
  }, [router]);

  return (
    <div className="fixed inset-0 bg-black z-50" onClick={handleScreenTap}>
      {/* 비디오 */}
      <video
        ref={videoRef}
        className="w-full h-full object-contain"
        playsInline
        autoPlay
        muted
      />

      {/* 에러 오버레이 */}
      {error && (
        <div className="absolute inset-0 bg-black/80 text-white flex items-center justify-center">
          {error}
        </div>
      )}

      {/* 컨트롤 오버레이 */}
      <div
        className={`absolute inset-0 transition-opacity duration-300 ${
          showOverlay ? 'opacity-100' : 'opacity-0 pointer-events-none'
        }`}
      >
        {/* 상단 */}
        <div className="absolute top-0 left-0 right-0 bg-gradient-to-b from-black/70 to-transparent p-4 flex items-center gap-3">
          <button
            onClick={(e) => { e.stopPropagation(); handleBack(); }}
            className="flex items-center gap-2 text-white hover:text-white/80"
          >
            <svg xmlns="http://www.w3.org/2000/svg" width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <path d="M15 18l-6-6 6-6" />
            </svg>
            <span className="text-lg font-medium">뒤로가기</span>
          </button>
          {badge && <div className="ml-auto">{badge}</div>}
        </div>

        {/* 하단 */}
        <div className="absolute bottom-0 left-0 right-0 bg-gradient-to-t from-black/70 to-transparent p-6">
          <h2 className="text-white text-xl font-bold">{title}</h2>
          {subtitle && <p className="text-white/70 text-sm mt-1">{subtitle}</p>}
          {bottomExtra && <div className="mt-2">{bottomExtra}</div>}
        </div>
      </div>
    </div>
  );
}
