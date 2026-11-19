package com.wjys.module.maintenance.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record MaintenanceRecordVO(Long id, String recordCode, Long planId, Long equipId, LocalDate maintainDate,
                                  Long maintainerId, String maintainContent, Integer maintainResult,
                                  BigDecimal cost, Integer downtimeMinutes, List<Long> fileIds, Integer status,
                                  Long auditUser, LocalDateTime auditTime, String remark, LocalDateTime createdTime) { }
