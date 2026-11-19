package com.wjys.module.inspection.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record TaskVO(Long id, String taskNo, Long planId, String planName, LocalDate planTime, Long assigneeId, Integer status, String statusLabel, Integer totalItems, Integer abnormalCount, LocalDateTime finishTime, List<TaskItemVO> items) { }
