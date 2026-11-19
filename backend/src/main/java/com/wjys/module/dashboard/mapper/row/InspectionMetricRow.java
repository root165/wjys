package com.wjys.module.dashboard.mapper.row;

import lombok.Data;

@Data
public class InspectionMetricRow {
    private Long taskTotal;
    private Long taskFinish;
    private Long taskTimeout;
    private Long abnormalCount;
    private Long checkedItems;
}
