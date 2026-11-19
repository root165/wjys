package com.wjys.module.repair.vo;

import java.time.LocalDateTime;
import java.math.BigDecimal;

public record WorkOrderVO(Long id, Long repairId, String orderNo, Long equipId, Long assigneeId, Integer status,
                          String dispatchRemark, LocalDateTime expectFinishTime, String finishReason,
                          String repairPlan, String finishResult, Integer downtimeMinutes, String acceptImages,
                          String rejectReason, LocalDateTime finishedTime, LocalDateTime acceptedTime, BigDecimal spareCost) {
}
