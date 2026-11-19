package com.wjys.module.dashboard.vo;

import java.math.BigDecimal;

public record InspectionSummaryVO(BigDecimal finishRate, BigDecimal timeoutRate, BigDecimal abnormalRate) { }
