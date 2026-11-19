package com.wjys.module.system.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SysUserRoleMapper {
    @Delete("delete from sys_user_role where user_id = #{userId}")
    int deleteByUserId(Long userId);

    @Insert("<script><foreach collection='roleIds' item='roleId' separator=';'>insert into sys_user_role (user_id, role_id) values (#{userId}, #{roleId})</foreach></script>")
    int insertLinks(@Param("userId") Long userId, @Param("roleIds") List<Long> roleIds);
}
