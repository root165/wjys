package com.wjys.module.dashboard.vo;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailyTrendVO(LocalDate statDate, Integer taskTotal, Integer taskFinish, Integer abnormalCount,
                           Integer faultCount, Integer downtimeMinutes, BigDecimal healthScore) { }
