package com.wjys.module.maintenance.vo;

import java.time.LocalDate;

public record MaintenancePlanVO(Long id, String planCode, String planName, Long equipId, Integer maintainType,
                                Integer cycleType, Integer cycleValue, String content, Long assigneeId,
                                LocalDate nextDate, Integer status, Long creatorId) { }
