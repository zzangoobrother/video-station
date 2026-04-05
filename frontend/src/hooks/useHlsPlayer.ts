'use client';

import { useEffect, useRef, useState } from 'react';
import Hls from 'hls.js';

export function useHlsPlayer(hlsUrl: string | null) {
  const videoRef = useRef<HTMLVideoElement>(null);
  const hlsRef = useRef<Hls | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setError(null);
    const video = videoRef.current;
    if (!video || !hlsUrl) return;

    if (Hls.isSupported()) {
      const hls = new Hls();
      hlsRef.current = hls;
      hls.loadSource(hlsUrl);
      hls.attachMedia(video);
      hls.on(Hls.Events.ERROR, (_, data) => {
        if (data.fatal) {
          setError('영상 로드에 실패했습니다.');
        }
      });

      return () => {
        hls.destroy();
        hlsRef.current = null;
      };
    } else if (video.canPlayType('application/vnd.apple.mpegurl')) {
      video.src = hlsUrl;
      return () => {
        video.removeAttribute('src');
        video.load();
      };
    } else {
      setError('이 브라우저에서는 HLS 재생을 지원하지 않습니다.');
    }
  }, [hlsUrl]);

  return { videoRef, error };
}
