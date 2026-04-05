export function formatDuration(seconds: number | null | undefined): string {
  if (seconds == null) return '-';
  const min = Math.floor(seconds / 60);
  const sec = seconds % 60;
  return sec > 0 ? `${min}분 ${sec}초` : `${min}분`;
}

export function formatDate(dateStr: string): string {
  return new Date(dateStr).toLocaleDateString('ko-KR');
}

export function formatFileSize(bytes: number | null | undefined): string {
  if (bytes == null) return '-';
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}
