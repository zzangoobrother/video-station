package com.videostation.application;

import com.videostation.application.dto.ChannelRequest;
import com.videostation.application.dto.ChannelResponse;
import com.videostation.domain.Channel;
import com.videostation.domain.User;
import com.videostation.global.error.BusinessException;
import com.videostation.global.error.ErrorCode;
import com.videostation.persistence.ChannelRepository;
import com.videostation.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ChannelService {

    private final ChannelRepository channelRepository;
    private final UserRepository userRepository;

    @Transactional
    public ChannelResponse create(ChannelRequest request, Long ownerId) {
        if (channelRepository.existsByName(request.name())) {
            throw new BusinessException(ErrorCode.DUPLICATE_CHANNEL_NAME);
        }
        User owner = userRepository.getReferenceById(ownerId);
        Channel channel = Channel.create(request.name(), request.description(), owner);
        return ChannelResponse.from(channelRepository.save(channel));
    }

    public Page<ChannelResponse> getChannels(Pageable pageable) {
        return channelRepository.findAll(pageable).map(ChannelResponse::from);
    }

    public ChannelResponse getChannel(Long channelId) {
        return ChannelResponse.from(findChannel(channelId));
    }

    Channel findChannel(Long channelId) {
        return channelRepository.findById(channelId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHANNEL_NOT_FOUND));
    }
}
