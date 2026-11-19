package com.wjys.module.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
        @NotBlank(message = "姓名不能为空") @Size(max = 64, message = "姓名不能超过 64 个字符") String nickname,
        @Size(max = 32, message = "手机号不能超过 32 个字符") String phone,
        @Email(message = "邮箱格式不正确") @Size(max = 128, message = "邮箱不能超过 128 个字符") String email,
        Long avatarFileId
) {
}
