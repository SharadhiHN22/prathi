package com.editshare.service;

import com.editshare.entity.Clip;
import com.editshare.repository.ClipRepository;
import com.editshare.repository.ClipVersionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CleanupSchedulerService {

    private static final Logger log = LoggerFactory.getLogger(CleanupSchedulerService.class);
    private final ClipRepository clipRepository;
    private final ClipVersionRepository clipVersionRepository;

    public CleanupSchedulerService(ClipRepository clipRepository, ClipVersionRepository clipVersionRepository) {
        this.clipRepository = clipRepository;
        this.clipVersionRepository = clipVersionRepository;
    }

    @Scheduled(cron = "${editshare.cleanup.cron:0 */5 * * * *}")
    @Transactional
    public void purgeExpiredClips() {
        LocalDateTime now = LocalDateTime.now();
        List<Clip> expiredClips = clipRepository.findByExpiresAtNotNullAndExpiresAtLessThanEqual(now);
        if (!expiredClips.isEmpty()) {
            for (Clip clip : expiredClips) {
                clipVersionRepository.deleteByClipId(clip.getId());
                clipRepository.delete(clip);
            }
            log.info("Expired clips cleanup executed: purged {} clips and their versions at {}", expiredClips.size(), now);
        }
    }
}
