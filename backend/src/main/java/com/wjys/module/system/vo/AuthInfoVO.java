package com.wjys.module.system.vo;

import java.util.List;

public record AuthInfoVO(UserInfoVO user, List<MenuVO> menus) {
}
