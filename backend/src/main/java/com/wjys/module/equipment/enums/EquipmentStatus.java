package com.wjys.module.equipment.enums;

import java.util.Arrays;

public enum EquipmentStatus {
    IDLE(0, "闲置"),
    RUNNING(1, "运行中"),
    MAINTENANCE(2, "保养中"),
    FAULT(3, "故障"),
    SCRAPPED(4, "已报废");

    private final int code;
    private final String label;

    EquipmentStatus(int code, String label) { this.code = code; this.label = label; }
    public int getCode() { return code; }
    public String getLabel() { return label; }
    public static EquipmentStatus fromCode(Integer code) { return Arrays.stream(values()).filter(item -> item.code == code).findFirst().orElseThrow(() -> new IllegalArgumentException("设备状态无效")); }
}
