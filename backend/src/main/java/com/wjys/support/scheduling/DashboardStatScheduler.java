package com.wjys.support.scheduling;

import com.wjys.module.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wjys.scheduling.enabled", havingValue = "true")
public class DashboardStatScheduler {
    private final DashboardService dashboardService;

    @Scheduled(cron = "${wjys.dashboard.daily-stat-cron:0 10 1 * * *}", zone = "Asia/Shanghai")
    public void aggregateYesterday() {
        dashboardService.refresh(LocalDate.now().minusDays(1));
    }
}
