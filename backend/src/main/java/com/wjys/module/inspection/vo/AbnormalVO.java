package com.wjys.module.inspection.vo;

import java.time.LocalDateTime;

public record AbnormalVO(Long id, Long taskId, Long taskItemId, Long equipId, Long pointId, Integer severity, String description, Integer status, LocalDateTime createdTime) { }
