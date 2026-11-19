package com.wjys.module.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.wjys.module.system.entity.SysUser;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

import com.wjys.module.inspection.vo.InspectionAssigneeVO;

@Mapper
public interface SysUserMapper extends BaseMapper<SysUser> {
    @Select("select * from sys_user where username = #{username} limit 1")
    SysUser selectByUsername(String username);

    @Select("select r.role_code from sys_role r join sys_user_role ur on ur.role_id = r.id where ur.user_id = #{userId} and r.status = 1")
    List<String> selectRoleCodes(Long userId);

    @Select("select role_id from sys_user_role where user_id = #{userId} order by role_id")
    List<Long> selectRoleIds(Long userId);

    @Select("select u.id, u.username, u.nickname, u.dept_id, d.dept_name from sys_user u join sys_user_role ur on ur.user_id = u.id join sys_role r on r.id = ur.role_id left join sys_dept d on d.id = u.dept_id where u.status = 1 and r.status = 1 and r.role_code = 'inspector' order by d.sort_no, u.id")
    List<InspectionAssigneeVO> selectActiveInspectors();

    @Select("select count(*) > 0 from sys_user u join sys_user_role ur on ur.user_id = u.id join sys_role r on r.id = ur.role_id where u.id = #{userId} and u.status = 1 and r.status = 1 and r.role_code = 'inspector'")
    boolean isActiveInspector(Long userId);

    @Select("select u.id from sys_user u join sys_user_role ur on ur.user_id = u.id join sys_role r on r.id = ur.role_id where u.status = 1 and u.dept_id = #{deptId} and r.status = 1 and r.role_code = 'inspector' order by u.id")
    List<Long> selectActiveInspectorIdsByDept(Long deptId);

    @Select("select distinct m.permission_code from sys_menu m join sys_role_menu rm on rm.menu_id = m.id join sys_user_role ur on ur.role_id = rm.role_id where ur.user_id = #{userId} and m.status = 1 and m.permission_code is not null")
    List<String> selectPermissionCodes(Long userId);

    @Update("update sys_user set token_version = token_version + 1, updated_time = current_timestamp(3) where id = #{userId}")
    int incrementTokenVersion(Long userId);
}
