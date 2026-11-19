package com.wjys.module.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysRoleMenuMapper {
    @Delete("delete from sys_role_menu where role_id = #{roleId}")
    int deleteByRoleId(Long roleId);

    @Insert("<script><foreach collection='menuIds' item='menuId' separator=';'>insert into sys_role_menu (role_id, menu_id) values (#{roleId}, #{menuId})</foreach></script>")
    int insertLinks(@Param("roleId") Long roleId, @Param("menuIds") List<Long> menuIds);
}
