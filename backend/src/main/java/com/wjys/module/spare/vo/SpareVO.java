package com.wjys.module.spare.vo;

import java.math.BigDecimal;

public record SpareVO(Long id, String spareCode, String spareName, String specification, String unit, Integer stockQty,
                      Integer warningQty, BigDecimal unitPrice, Integer status, boolean warning) {
}
