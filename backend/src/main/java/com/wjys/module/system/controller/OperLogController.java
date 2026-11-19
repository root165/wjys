package com.wjys.module.system.controller;

import com.wjys.common.model.ApiResponse;
import com.wjys.common.model.PageResult;
import com.wjys.module.system.service.OperLogService;
import com.wjys.module.system.vo.OperLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system/logs")
@RequiredArgsConstructor
@PreAuthorize("hasRole('system_admin')")
public class OperLogController {
    private final OperLogService logService;

    @GetMapping
    public ApiResponse<PageResult<OperLogVO>> page(@RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(logService.page(pageNum, pageSize));
    }
}
