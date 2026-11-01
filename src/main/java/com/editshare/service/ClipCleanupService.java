package com.editshare.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class ClipCleanupService {

    private static final Logger log = LoggerFactory.getLogger(ClipCleanupService.class);
    private final ClipService clipService;

    public ClipCleanupService(ClipService clipService) {
        this.clipService = clipService;
    }

    /**
     * Periodically runs cleanup to purge expired clips and free up URL slugs for reuse.
     * Defaults to every 5 minutes (configurable via CLEANUP_CRON or editshare.cleanup.cron).
     */
    @Scheduled(cron = "${editshare.cleanup.cron:0 */5 * * * *}")
    public void runScheduledCleanup() {
        try {
            log.info("Running scheduled expired clips cleanup...");
            clipService.purgeExpiredClips();
        } catch (Exception e) {
            log.error("Error during scheduled expired clips cleanup: {}", e.getMessage(), e);
        }
    }
}
