package com.wjys.module.inspection.vo;

import java.math.BigDecimal;

public record TaskItemVO(Long id, Long pointId, Long itemId, Long equipId, String equipCode, String pointCode, String itemCode, String itemName, String valueType, String checkMethod, String unit, BigDecimal minValue, BigDecimal maxValue, String optionsJson, Integer requiredPhoto, Integer checkResult, String checkValue, String remark, String abnormalReason) { }
