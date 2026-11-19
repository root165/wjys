package com.wjys.support.file.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record BindFilesRequest(@NotEmpty List<Long> fileIds, @NotBlank String bizType, @NotNull Long bizId) { }
