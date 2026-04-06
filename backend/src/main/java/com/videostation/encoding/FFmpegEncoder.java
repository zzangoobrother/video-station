package com.videostation.encoding;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class FFmpegEncoder {

    @Value("${storage.encoded-path}")
    private String encodedPath;

    @Value("${storage.thumbnails-path}")
    private String thumbnailsPath;

    @Value("${encoding.ffmpeg-path}")
    private String ffmpegPath;

    public String[] buildEncodeCommand(Long videoId, String inputPath) {
        String outputDir = encodedPath + "/" + videoId;
        String outputFile = outputDir + "/master.m3u8";
        String segmentPattern = outputDir + "/segment_%03d.ts";

        return new String[]{
                ffmpegPath,
                "-i", inputPath,
                // 오디오가 없는 영상용 무음 트랙
                "-f", "lavfi", "-i", "anullsrc=r=44100:cl=stereo",
                // 영상: 1080p 스케일링, 30fps, 키프레임 2초 간격
                "-vf", "scale=-2:1080",
                "-c:v", "libx264", "-preset", "medium", "-crf", "23",
                "-r", "30",
                "-g", "60", "-keyint_min", "60", "-sc_threshold", "0",
                // 오디오: AAC (원본 오디오 있으면 사용, 없으면 무음)
                "-c:a", "aac", "-b:a", "128k",
                "-shortest",
                "-hls_time", "6", "-hls_list_size", "0",
                "-hls_segment_filename", segmentPattern,
                "-f", "hls", outputFile
        };
    }

    public String[] buildThumbnailCommand(Long videoId, String inputPath) {
        String outputFile = thumbnailsPath + "/" + videoId + ".jpg";

        return new String[]{
                ffmpegPath, "-i", inputPath,
                "-ss", "00:00:30", "-frames:v", "1", "-q:v", "2",
                outputFile
        };
    }

    public String[] buildDurationCommand(String inputPath) {
        return new String[]{
                ffmpegPath, "-i", inputPath,
                "-hide_banner", "-f", "null", "-"
        };
    }

    public String getHlsPath(Long videoId) {
        return encodedPath + "/" + videoId + "/master.m3u8";
    }

    public String getThumbnailPath(Long videoId) {
        return thumbnailsPath + "/" + videoId + ".jpg";
    }

    public int execute(String[] command) throws Exception {
        String outputDir = Path.of(command[command.length - 1]).getParent().toString();
        Files.createDirectories(Path.of(outputDir));

        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();

        try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            while (reader.readLine() != null) {
                // FFmpeg 출력 소비
            }
        }

        return process.waitFor();
    }

    public Integer probeDuration(String inputPath) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    ffmpegPath, "-i", inputPath, "-hide_banner"
            );
            pb.redirectErrorStream(true);
            Process process = pb.start();

            String output;
            try (var reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                output = String.join("\n", reader.lines().toList());
            }
            process.waitFor();

            // "Duration: HH:MM:SS.ms" 파싱
            var matcher = java.util.regex.Pattern.compile("Duration: (\\d{2}):(\\d{2}):(\\d{2})")
                    .matcher(output);
            if (matcher.find()) {
                int hours = Integer.parseInt(matcher.group(1));
                int minutes = Integer.parseInt(matcher.group(2));
                int seconds = Integer.parseInt(matcher.group(3));
                return hours * 3600 + minutes * 60 + seconds;
            }
        } catch (Exception e) {
            // duration 추출 실패 시 null 반환
        }
        return null;
    }
}
