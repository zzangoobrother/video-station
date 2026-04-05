package com.videostation.encoding;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class FFmpegEncoderTest {

    private FFmpegEncoder encoder;

    @BeforeEach
    void setUp() {
        encoder = new FFmpegEncoder();
        ReflectionTestUtils.setField(encoder, "encodedPath", "/data/videos/encoded");
        ReflectionTestUtils.setField(encoder, "thumbnailsPath", "/data/videos/thumbnails");
        ReflectionTestUtils.setField(encoder, "ffmpegPath", "ffmpeg");
    }

    @Test
    @DisplayName("인코딩 명령어 생성")
    void buildEncodeCommand() {
        String[] command = encoder.buildEncodeCommand(1L, "/data/originals/test.mp4");

        assertThat(command).contains("ffmpeg");
        assertThat(command).contains("-i", "/data/originals/test.mp4");
        assertThat(command).contains("-f", "hls");
        assertThat(String.join(" ", command)).contains("/data/videos/encoded/1/master.m3u8");
    }

    @Test
    @DisplayName("썸네일 추출 명령어 생성")
    void buildThumbnailCommand() {
        String[] command = encoder.buildThumbnailCommand(1L, "/data/originals/test.mp4");

        assertThat(command).contains("ffmpeg");
        assertThat(command).contains("-ss", "00:00:30");
        assertThat(String.join(" ", command)).contains("/data/videos/thumbnails/1.jpg");
    }

    @Test
    @DisplayName("출력 경로 생성")
    void getOutputPaths() {
        assertThat(encoder.getHlsPath(1L)).isEqualTo("/data/videos/encoded/1/master.m3u8");
        assertThat(encoder.getThumbnailPath(1L)).isEqualTo("/data/videos/thumbnails/1.jpg");
    }
}
