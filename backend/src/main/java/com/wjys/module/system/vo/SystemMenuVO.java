package com.wjys.module.system.vo;

public record SystemMenuVO(Long id, Long parentId, String menuName, String routePath, String component, String permissionCode, Integer menuType, Integer sortNo) {
}
