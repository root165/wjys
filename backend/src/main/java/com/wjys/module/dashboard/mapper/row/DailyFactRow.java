package com.wjys.module.dashboard.mapper.row;

import lombok.Data;

@Data
public class DailyFactRow {
    private Long equipId;
    private Integer taskTotal;
    private Integer taskFinish;
    private Integer abnormalCount;
    private Integer checkedItems;
    private Integer faultCount;
    private Integer downtimeMinutes;
}
