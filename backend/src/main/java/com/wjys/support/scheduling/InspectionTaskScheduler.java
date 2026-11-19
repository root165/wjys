package com.wjys.support.scheduling;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.module.inspection.entity.InsPlan;
import com.wjys.module.inspection.entity.InsTask;
import com.wjys.module.inspection.enums.InspectionTaskStatus;
import com.wjys.module.inspection.mapper.InsPlanMapper;
import com.wjys.module.inspection.mapper.InsTaskMapper;
import com.wjys.module.inspection.service.TaskGenerateService;
import com.wjys.support.lock.InspectionGenerationLock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "wjys.scheduling.enabled", havingValue = "true")
public class InspectionTaskScheduler {
    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");
    private final InsPlanMapper planMapper;
    private final InsTaskMapper taskMapper;
    private final com.wjys.module.inspection.service.PlanService planService;
    private final TaskGenerateService taskGenerateService;
    private final InspectionGenerationLock generationLock;

    @Scheduled(cron = "${wjys.inspection.daily-generate-cron:0 * * * * *}", zone = "Asia/Shanghai")
    public void generateDailyTasks() {
        String lockToken = generationLock.tryAcquire();
        if (lockToken == null) return;
        LocalDate planTime = LocalDate.now(ZONE_ID);
        LocalTime now = LocalTime.now(ZONE_ID);
        try {
            for (InsPlan plan : planMapper.selectList(new LambdaQueryWrapper<InsPlan>().eq(InsPlan::getStatus, 1))) {
                if (plan.getExecuteTime() != null && now.isBefore(plan.getExecuteTime())) continue;
                if (!planService.isScheduledDate(plan, planTime)) continue;
                try {
                    taskGenerateService.generate(plan.getId(), planTime);
                } catch (RuntimeException exception) {
                    log.warn("巡检计划自动生成失败，planId={}, planTime={}", plan.getId(), planTime, exception);
                }
            }
            markOverdue(planTime, now);
        } finally {
            generationLock.release(lockToken);
        }
    }

    private void markOverdue(LocalDate today, LocalTime now) {
        // 逾期判断覆盖已停用计划的历史任务；停用计划不再生成新任务，但不能让已有任务永远停留在待领取/执行中。
        for (InsPlan plan : planMapper.selectList(new LambdaQueryWrapper<>())) {
            LocalTime cutoff = (plan.getExecuteTime() == null ? LocalTime.of(9, 0) : plan.getExecuteTime()).plusMinutes(30);
            List<InsTask> tasks = taskMapper.selectList(new LambdaQueryWrapper<InsTask>()
                    .eq(InsTask::getPlanId, plan.getId())
                    .in(InsTask::getStatus, InspectionTaskStatus.PENDING.getCode(), InspectionTaskStatus.RECEIVED.getCode()));
            for (InsTask task : tasks) {
                boolean oldDate = task.getPlanTime().isBefore(today);
                boolean todayExpired = task.getPlanTime().equals(today) && now.isAfter(cutoff);
                if (oldDate || todayExpired) {
                    taskMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<InsTask>()
                            .eq(InsTask::getId, task.getId())
                            .in(InsTask::getStatus, InspectionTaskStatus.PENDING.getCode(), InspectionTaskStatus.RECEIVED.getCode())
                            .set(InsTask::getStatus, InspectionTaskStatus.OVERDUE.getCode()));
                }
            }
        }
    }

}
