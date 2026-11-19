package com.wjys.module.equipment.vo;

import java.time.LocalDateTime;

public record StatusLogVO(Long id, Integer fromStatus, Integer toStatus, String sourceType, Long sourceId, String reason, LocalDateTime createdTime) { }
