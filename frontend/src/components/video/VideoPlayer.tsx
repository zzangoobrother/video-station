'use client';

import { useHlsPlayer } from '@/hooks/useHlsPlayer';

interface VideoPlayerProps {
  hlsUrl: string | null;
  onEnded?: () => void;
}

export default function VideoPlayer({ hlsUrl, onEnded }: VideoPlayerProps) {
  const { videoRef, error } = useHlsPlayer(hlsUrl);

  if (error) {
    return <div className="bg-black text-white flex items-center justify-center aspect-video rounded">{error}</div>;
  }

  if (!hlsUrl) {
    return <div className="bg-gray-900 flex items-center justify-center aspect-video rounded text-gray-400">영상을 준비 중입니다.</div>;
  }

  return (
    <video
      ref={videoRef}
      controls
      className="w-full aspect-video bg-black rounded"
      onEnded={onEnded}
    />
  );
}
