package com.wjys.module.dashboard.vo;

import java.math.BigDecimal;

public record FaultParetoItemVO(String name, Long count, BigDecimal ratio, BigDecimal cumulative) { }
