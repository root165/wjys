package com.wjys.module.mobile.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record MobileSyncPushRequest(@NotEmpty List<@Valid MobileSyncPushItem> items) { }
