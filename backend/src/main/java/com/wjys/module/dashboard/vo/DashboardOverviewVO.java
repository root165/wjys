package com.wjys.module.dashboard.vo;

import java.math.BigDecimal;
import java.util.Map;

public record DashboardOverviewVO(Long equipTotal, Map<Integer, Long> statusDist, Long taskToday,
                                  BigDecimal taskFinishRate, Long unhandledRepair, Long faultToday,
                                  Long spareAlerts) { }
