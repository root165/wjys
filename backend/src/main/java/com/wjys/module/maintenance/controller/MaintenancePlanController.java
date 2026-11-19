package com.wjys.module.maintenance.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.maintenance.dto.ChangeMaintenancePlanStatusRequest;
import com.wjys.module.maintenance.dto.SaveMaintenancePlanRequest;
import com.wjys.module.maintenance.service.MaintenancePlanService;
import com.wjys.module.maintenance.vo.MaintenancePlanVO;
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

import java.util.List;

@RestController
@RequestMapping("/api/maintenance/plans")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('maintenance:view')")
public class MaintenancePlanController {
    private final MaintenancePlanService planService;

    @GetMapping
    public ApiResponse<List<MaintenancePlanVO>> list(@RequestParam(required = false) Integer status) {
        return ApiResponse.success(planService.list(status));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('maintenance:write') or hasRole('system_admin')")
    @OperLog("创建保养计划")
    public ApiResponse<MaintenancePlanVO> create(@Valid @RequestBody SaveMaintenancePlanRequest request,
                                                   @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(planService.create(request, principal.id()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('maintenance:write') or hasRole('system_admin')")
    @OperLog("更新保养计划")
    public ApiResponse<MaintenancePlanVO> update(@PathVariable Long id, @Valid @RequestBody SaveMaintenancePlanRequest request) {
        return ApiResponse.success(planService.update(id, request));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('maintenance:write') or hasRole('system_admin')")
    @OperLog("切换保养计划状态")
    public ApiResponse<MaintenancePlanVO> changeStatus(@PathVariable Long id,
                                                        @Valid @RequestBody ChangeMaintenancePlanStatusRequest request) {
        return ApiResponse.success(planService.changeStatus(id, request));
    }
}
