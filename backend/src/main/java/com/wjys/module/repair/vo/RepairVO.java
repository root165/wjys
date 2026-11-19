package com.wjys.module.repair.vo;

import java.time.LocalDateTime;

public record RepairVO(Long id, Long equipId, String sourceType, Long sourceId, String description, Long reporterId,
                       Integer status, LocalDateTime createdTime) {
}
