package com.videostation.application;

import com.videostation.application.dto.VideoResponse;
import com.videostation.application.dto.VideoUpdateRequest;
import com.videostation.domain.User;
import com.videostation.domain.Video;
import com.videostation.domain.constant.VideoStatus;
import com.videostation.event.VideoUploadedEvent;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.UserRepository;
import com.videostation.persistence.VideoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VideoService {

    private final VideoRepository videoRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public VideoResponse upload(MultipartFile file, String title, String description, String tags, Long uploaderId) {
        User uploader = userRepository.getReferenceById(uploaderId);
        Path filePath = fileStorageService.storeOriginal(file);

        Video video = Video.create(title, description, tags,
                filePath.toString(), file.getOriginalFilename(), file.getSize(), uploader);
        video.changeStatus(VideoStatus.ENCODING_QUEUED);
        Video saved = videoRepository.save(video);

        eventPublisher.publishEvent(new VideoUploadedEvent(saved.getId(), saved.getOriginalFilePath()));

        return VideoResponse.from(saved);
    }

    public Page<VideoResponse> getAdminVideos(VideoStatus status, String keyword, Pageable pageable) {
        Page<Video> page;
        if (status != null) {
            page = videoRepository.findByStatus(status, pageable);
        } else if (keyword != null && !keyword.isBlank()) {
            page = videoRepository.findByTitleContaining(keyword, pageable);
        } else {
            page = videoRepository.findAll(pageable);
        }
        return page.map(VideoResponse::from);
    }

    public VideoResponse getVideo(Long videoId) {
        return VideoResponse.from(findVideo(videoId));
    }

    @Transactional
    public VideoResponse updateVideo(Long videoId, VideoUpdateRequest request) {
        Video video = findVideo(videoId);
        video.updateMetadata(request.title(), request.description(), request.tags());
        return VideoResponse.from(video);
    }

    @Transactional
    public void deleteVideo(Long videoId) {
        Video video = findVideo(videoId);
        video.changeStatus(VideoStatus.DELETED);
    }

    @Transactional
    public VideoResponse toggleVisibility(Long videoId) {
        Video video = findVideo(videoId);
        video.toggleVisibility();
        return VideoResponse.from(video);
    }

    private Video findVideo(Long videoId) {
        return videoRepository.findById(videoId)
                .orElseThrow(() -> new BusinessException(ErrorCode.VIDEO_NOT_FOUND));
    }
}
