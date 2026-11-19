package com.wjys.module.repair.dto;

import jakarta.validation.constraints.NotBlank;

public record RejectRequest(@NotBlank String reason) {
}
