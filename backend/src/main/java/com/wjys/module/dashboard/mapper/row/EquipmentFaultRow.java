package com.wjys.module.dashboard.mapper.row;

import lombok.Data;

@Data
public class EquipmentFaultRow {
    private Long equipId;
    private String equipCode;
    private String equipName;
    private Long faultCount;
    private Long downtimeMinutes;
}
