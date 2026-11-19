package com.wjys.module.system.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.system.dto.SaveDeptRequest;
import com.wjys.module.system.service.DeptService;
import com.wjys.module.system.vo.DeptVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/system/depts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('system_admin')")
public class DeptController {
    private final DeptService deptService;

    @GetMapping
    public ApiResponse<List<DeptVO>> list() { return ApiResponse.success(deptService.list()); }

    @PostMapping
    @OperLog("创建部门")
    public ApiResponse<DeptVO> create(@Valid @RequestBody SaveDeptRequest request) { return ApiResponse.success("部门创建成功", deptService.create(request)); }

    @PutMapping("/{id}")
    @OperLog("编辑部门")
    public ApiResponse<DeptVO> update(@PathVariable Long id, @Valid @RequestBody SaveDeptRequest request) { return ApiResponse.success("部门已更新", deptService.update(id, request)); }

    @DeleteMapping("/{id}")
    @OperLog("删除部门")
    public ApiResponse<Void> delete(@PathVariable Long id) { deptService.delete(id); return ApiResponse.success("部门已删除", null); }
}
