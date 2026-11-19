package com.wjys.module.system.vo;

import java.time.LocalDateTime;

public record OperLogVO(Long id, Long userId, String username, String operation, String requestMethod, String requestUri, Integer success, String traceId, LocalDateTime createdTime) {
}
