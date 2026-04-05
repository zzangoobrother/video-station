package com.videostation.persistence.support.objectstorage;

import com.videostation.domain.Video;
import com.videostation.persistence.VideoRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.nio.file.Path;

@Slf4j
@Service
public class ObjectStorageService {

    private final VideoRepository videoRepository;
    private final boolean enabled;
    private final String bucket;
    private final S3Client s3Client;

    public ObjectStorageService(
            VideoRepository videoRepository,
            @Value("${ncp.object-storage.enabled}") boolean enabled,
            @Value("${ncp.object-storage.endpoint}") String endpoint,
            @Value("${ncp.object-storage.region}") String region,
            @Value("${ncp.object-storage.access-key}") String accessKey,
            @Value("${ncp.object-storage.secret-key}") String secretKey,
            @Value("${ncp.object-storage.bucket}") String bucket
    ) {
        this.videoRepository = videoRepository;
        this.enabled = enabled;
        this.bucket = bucket;

        if (enabled) {
            this.s3Client = S3Client.builder()
                    .endpointOverride(URI.create(endpoint))
                    .region(Region.of(region))
                    .credentialsProvider(StaticCredentialsProvider.create(
                            AwsBasicCredentials.create(accessKey, secretKey)))
                    .forcePathStyle(true)
                    .build();
        } else {
            this.s3Client = null;
        }
    }

    @Async
    @Transactional
    public void backupOriginal(Long videoId, String originalFilePath) {
        if (!enabled) {
            log.debug("Object Storage 비활성 상태, 백업 생략: videoId={}", videoId);
            return;
        }

        try {
            String key = "originals/" + videoId + "/" + Path.of(originalFilePath).getFileName();

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .build(),
                    Path.of(originalFilePath)
            );

            videoRepository.findById(videoId).ifPresent(video -> video.setObjectStorageKey(key));
            log.info("Object Storage 백업 완료: videoId={}, key={}", videoId, key);
        } catch (Exception e) {
            log.error("Object Storage 백업 실패: videoId={}", videoId, e);
        }
    }
}
