package com.wjys.module.dashboard.vo;

import java.math.BigDecimal;

public record EquipmentRankingItemVO(String equipCode, String equipName, Long faultCount,
                                     Long downtimeMinutes, BigDecimal mtbf, BigDecimal mttr,
                                     BigDecimal healthScore) { }
