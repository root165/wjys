package com.wjys.support.scheduling;

import com.wjys.support.file.FileCleanupService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wjys.scheduling.enabled", havingValue = "true")
public class FileCleanupScheduler {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final FileCleanupService fileCleanupService;

    @Scheduled(cron = "${wjys.file.cleanup-cron:0 0 * * * *}", zone = "Asia/Shanghai")
    public void cleanupExpiredFiles() {
        int deleted = fileCleanupService.cleanupExpiredUnboundFiles(LocalDateTime.now(ZONE_ID));
        if (deleted > 0) log.info("已清理过期未绑定文件，count={}", deleted);
    }
}
