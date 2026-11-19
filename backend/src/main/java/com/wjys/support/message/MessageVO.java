package com.wjys.support.message;

import java.time.LocalDateTime;

public record MessageVO(Long id, String messageType, String title, String content, String bizType,
                        Long bizId, Integer readFlag, LocalDateTime createdTime, LocalDateTime readTime) { }
