package com.wjys.module.inspection.vo;

public record TaskSubmitVO(Long taskId, String taskNo, Integer status, Integer abnormalCount, boolean idempotentReplay) { }
