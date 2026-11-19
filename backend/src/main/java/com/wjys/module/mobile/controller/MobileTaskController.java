package com.wjys.module.mobile.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.inspection.service.TaskService;
import com.wjys.module.inspection.vo.TaskVO;
import com.wjys.security.WjysPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/tasks")
@RequiredArgsConstructor
@PreAuthorize("hasRole('inspector')")
public class MobileTaskController {
    private final TaskService taskService;

    @PutMapping("/{id}/receive")
    @OperLog("移动端领取巡检任务")
    public ApiResponse<TaskVO> receive(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(taskService.receive(id, principal.id()));
    }
}
