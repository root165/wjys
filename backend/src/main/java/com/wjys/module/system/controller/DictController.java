package com.wjys.module.system.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.system.dto.SaveDictRequest;
import com.wjys.module.system.service.DictService;
import com.wjys.module.system.vo.DictVO;
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
@RequestMapping("/api/system/dicts")
@RequiredArgsConstructor
@PreAuthorize("hasRole('system_admin')")
public class DictController {
    private final DictService dictService;

    @GetMapping("/{dictType}")
    public ApiResponse<List<DictVO>> list(@PathVariable String dictType) { return ApiResponse.success(dictService.list(dictType)); }

    @PostMapping
    @OperLog("创建字典项")
    public ApiResponse<DictVO> create(@Valid @RequestBody SaveDictRequest request) { return ApiResponse.success("字典项创建成功", dictService.create(request)); }

    @PutMapping("/{id}")
    @OperLog("编辑字典项")
    public ApiResponse<DictVO> update(@PathVariable Long id, @Valid @RequestBody SaveDictRequest request) { return ApiResponse.success("字典项已更新", dictService.update(id, request)); }

    @DeleteMapping("/{id}")
    @OperLog("删除字典项")
    public ApiResponse<Void> delete(@PathVariable Long id) { dictService.delete(id); return ApiResponse.success("字典项已删除", null); }
}
