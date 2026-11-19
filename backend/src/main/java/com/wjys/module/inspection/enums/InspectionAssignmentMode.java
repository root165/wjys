package com.wjys.module.inspection.enums;

/**
 * How a generated inspection task obtains its executor.
 *
 * SELF_CLAIM is deliberately the default: the task is visible to eligible
 * inspectors with a null assignee and is claimed atomically on the mobile
 * client.
 */
public enum InspectionAssignmentMode {
    SELF_CLAIM,
    FIXED_USER,
    EQUIPMENT_MANAGER,
    TEAM_ROUND_ROBIN;

    public static InspectionAssignmentMode from(String value) {
        if (value == null || value.isBlank()) return SELF_CLAIM;
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
