package com.wjys.module.dashboard.vo;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DashboardExportRow {
    @ExcelProperty("设备编码")
    private String equipCode;
    @ExcelProperty("设备名称")
    private String equipName;
    @ExcelProperty("故障次数")
    private Long faultCount;
    @ExcelProperty("停机分钟")
    private Long downtimeMinutes;
    @ExcelProperty("MTBF（小时）")
    private BigDecimal mtbf;
    @ExcelProperty("MTTR（分钟）")
    private BigDecimal mttr;
    @ExcelProperty("健康分")
    private BigDecimal healthScore;
}
