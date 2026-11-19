package com.wjys.module.inspection.vo;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record PlanVO(Long id, String planCode, String planName, String cycleType, String cycleValue, LocalDate startDate, LocalDate endDate, LocalTime executeTime, String assignmentMode, Long assignmentDeptId, Long responsibleUserId, Integer status, List<Long> pointIds) { }
