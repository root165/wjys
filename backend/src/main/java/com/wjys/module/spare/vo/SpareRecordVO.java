package com.wjys.module.spare.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record SpareRecordVO(Long id, Long orderId, Long spareId, Long sourceRecordId, String direction, Integer quantity,
                            BigDecimal unitPrice, BigDecimal amount, Long operatorId, String remark, LocalDateTime createdTime) {
}
