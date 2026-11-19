package com.wjys.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.system.dto.SaveDeptRequest;
import com.wjys.module.system.entity.SysDept;
import com.wjys.module.system.mapper.SysDeptMapper;
import com.wjys.module.system.vo.DeptVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeptService {
    private final SysDeptMapper deptMapper;

    public List<DeptVO> list() {
        return deptMapper.selectList(new LambdaQueryWrapper<SysDept>().orderByAsc(SysDept::getSortNo, SysDept::getId)).stream().map(this::toVO).toList();
    }

    @Transactional
    public DeptVO create(SaveDeptRequest request) {
        long parentId = request.parentId() == null ? 0 : request.parentId();
        validateParent(parentId, null);
        SysDept dept = new SysDept();
        dept.setParentId(parentId);
        dept.setDeptName(request.deptName());
        dept.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        dept.setStatus(1);
        deptMapper.insert(dept);
        return toVO(dept);
    }

    @Transactional
    public DeptVO update(Long id, SaveDeptRequest request) {
        SysDept dept = getRequired(id);
        long parentId = request.parentId() == null ? 0 : request.parentId();
        validateParent(parentId, id);
        dept.setParentId(parentId);
        dept.setDeptName(request.deptName());
        dept.setSortNo(request.sortNo() == null ? 0 : request.sortNo());
        deptMapper.updateById(dept);
        return toVO(dept);
    }

    @Transactional
    public void delete(Long id) {
        getRequired(id);
        if (id == 1L) throw new BusinessException(HttpStatus.CONFLICT, "系统根部门不能删除");
        if (deptMapper.countChildren(id) > 0) throw new BusinessException(HttpStatus.CONFLICT, "存在下级部门，不能删除");
        if (deptMapper.countUsers(id) > 0) throw new BusinessException(HttpStatus.CONFLICT, "部门下存在用户，不能删除");
        deptMapper.deleteById(id);
    }

    private SysDept getRequired(Long id) {
        SysDept dept = deptMapper.selectById(id);
        if (dept == null) throw new BusinessException(HttpStatus.NOT_FOUND, "部门不存在");
        return dept;
    }

    private void validateParent(long parentId, Long selfId) {
        if (parentId == 0) return;
        if (selfId != null && parentId == selfId) throw new BusinessException(HttpStatus.BAD_REQUEST, "部门不能设置自身为上级");
        if (deptMapper.selectById(parentId) == null) throw new BusinessException(HttpStatus.BAD_REQUEST, "上级部门不存在");
    }

    private DeptVO toVO(SysDept dept) { return new DeptVO(dept.getId(), dept.getParentId(), dept.getDeptName(), dept.getSortNo(), dept.getStatus()); }
}
