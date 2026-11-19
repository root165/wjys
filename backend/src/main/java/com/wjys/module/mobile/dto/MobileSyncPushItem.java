package com.wjys.module.mobile.dto;

import com.wjys.module.inspection.dto.TaskSubmitRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record MobileSyncPushItem(@NotNull Long taskId, @NotNull @Valid TaskSubmitRequest payload) { }
