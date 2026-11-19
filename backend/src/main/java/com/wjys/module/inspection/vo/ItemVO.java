package com.wjys.module.inspection.vo;

import java.math.BigDecimal;

public record ItemVO(Long id, Long pointId, String itemCode, String itemName, String checkMethod, String valueType, BigDecimal minValue, BigDecimal maxValue, String unit, String optionsJson, Integer requiredPhoto, Integer status) { }
