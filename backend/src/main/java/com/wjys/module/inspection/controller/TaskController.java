package com.wjys.module.inspection.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.common.model.PageResult;
import com.wjys.module.inspection.dto.TaskQuery;
import com.wjys.module.inspection.dto.TaskSubmitRequest;
import com.wjys.module.inspection.service.TaskService;
import com.wjys.module.inspection.service.TaskSubmitService;
import com.wjys.module.inspection.vo.TaskSubmitVO;
import com.wjys.module.inspection.vo.TaskVO;
import com.wjys.security.WjysPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/inspection/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inspection:task:view')")
public class TaskController {
    private final TaskService taskService;
    private final TaskSubmitService taskSubmitService;
    @GetMapping public ApiResponse<PageResult<TaskVO>> page(@RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "20") long pageSize) { return ApiResponse.success(taskService.page(new TaskQuery(status, pageNum, pageSize))); }
    @GetMapping("/{id}") public ApiResponse<TaskVO> detail(@PathVariable Long id) { return ApiResponse.success(taskService.detail(id)); }
    @PutMapping("/{id}/receive") @PreAuthorize("hasRole('inspector')") @OperLog("领取巡检任务") public ApiResponse<TaskVO> receive(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(taskService.receive(id, principal.id())); }
    @PostMapping("/{id}/submit") @PreAuthorize("hasRole('inspector')") @OperLog("提交巡检任务") public ApiResponse<TaskSubmitVO> submit(@PathVariable Long id, @Valid @RequestBody TaskSubmitRequest request, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(taskSubmitService.submit(id, request, principal.id())); }
}
