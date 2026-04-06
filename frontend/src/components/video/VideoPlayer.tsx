'use client';

import { useHlsPlayer } from '@/hooks/useHlsPlayer';

interface VideoPlayerProps {
  hlsUrl: string | null;
  onEnded?: () => void;
}

export default function VideoPlayer({ hlsUrl, onEnded }: VideoPlayerProps) {
  const { videoRef, error } = useHlsPlayer(hlsUrl);

  if (!hlsUrl) {
    return <div className="bg-gray-900 flex items-center justify-center aspect-video rounded text-gray-400">영상을 준비 중입니다.</div>;
  }

  return (
    <div className="relative">
      <video
        ref={videoRef}
        controls
        autoPlay
        muted
        playsInline
        className="w-full aspect-video bg-black rounded"
        onEnded={onEnded}
      />
      {error && (
        <div className="absolute inset-0 bg-black/80 text-white flex items-center justify-center rounded">
          {error}
        </div>
      )}
    </div>
  );
}
