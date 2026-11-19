package com.wjys.module.maintenance.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AuditMaintenanceRecordRequest(@NotNull Boolean pass, @Size(max = 500) String remark) { }
