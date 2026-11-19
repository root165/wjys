package com.wjys.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.system.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {
    @Select("select m.id from sys_menu m join sys_role_menu rm on rm.menu_id = m.id where rm.role_id = #{roleId} and m.status = 1 order by m.sort_no, m.id")
    List<Long> selectMenuIds(Long roleId);

    @Select("select count(*) from sys_user_role where role_id = #{roleId}")
    long countUsers(Long roleId);

    @Select("select count(*) from sys_role_menu where role_id = #{roleId}")
    long countMenus(Long roleId);
}
