package com.wjys.module.inspection.enums;

public enum InspectionTaskStatus {
    PENDING(0), RECEIVED(1), COMPLETED(2), OVERDUE(3);

    private final int code;
    InspectionTaskStatus(int code) { this.code = code; }
    public int getCode() { return code; }
}
