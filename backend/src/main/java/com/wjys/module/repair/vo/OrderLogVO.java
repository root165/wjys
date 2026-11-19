package com.wjys.module.repair.vo;

import java.time.LocalDateTime;

public record OrderLogVO(Long id, Long orderId, String action, Long operatorId, String remark, LocalDateTime createdTime) {
}
