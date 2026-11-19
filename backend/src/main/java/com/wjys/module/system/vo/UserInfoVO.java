package com.wjys.module.system.vo;

import java.util.List;

public record UserInfoVO(Long id, String username, String nickname, String phone, String email, Long avatarFileId, List<String> roles, List<String> permissions) {
}
