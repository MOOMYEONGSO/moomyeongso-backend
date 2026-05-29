package org.example.moomyeongso.domain.postimage.scheduler;

import lombok.RequiredArgsConstructor;
import org.example.moomyeongso.domain.postimage.service.PostImageService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PostImageCleanupScheduler {

    private final PostImageService postImageService;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void cleanupExpiredImages() {
        postImageService.cleanupExpiredImages();
    }
}

