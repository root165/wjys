package com.wjys.module.maintenance.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.maintenance.dto.AuditMaintenanceRecordRequest;
import com.wjys.module.maintenance.dto.SaveMaintenanceRecordRequest;
import com.wjys.module.maintenance.service.MaintenanceRecordService;
import com.wjys.module.maintenance.vo.MaintenanceRecordVO;
import com.wjys.module.repair.vo.RepairVO;
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
@RequestMapping("/api/maintenance/records")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('maintenance:view')")
public class MaintenanceRecordController {
    private final MaintenanceRecordService recordService;

    @GetMapping
    public ApiResponse<List<MaintenanceRecordVO>> list(@RequestParam(required = false) Integer status,
                                                        @RequestParam(required = false) Long equipId) {
        return ApiResponse.success(recordService.list(status, equipId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('maintenance:write') or hasRole('system_admin')")
    @OperLog("提交维保记录")
    public ApiResponse<MaintenanceRecordVO> submit(@Valid @RequestBody SaveMaintenanceRecordRequest request,
                                                    @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(recordService.submit(request, principal.id()));
    }

    @PutMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('maintenance:audit') or hasRole('system_admin')")
    @OperLog("审核维保记录")
    public ApiResponse<MaintenanceRecordVO> audit(@PathVariable Long id, @Valid @RequestBody AuditMaintenanceRecordRequest request,
                                                   @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(recordService.audit(id, request, principal.id()));
    }

    @PostMapping("/{id}/to-repair")
    @PreAuthorize("hasAuthority('repair:create') or hasRole('system_admin')")
    @OperLog("维保异常转报修")
    public ApiResponse<RepairVO> toRepair(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(recordService.toRepair(id, principal.id()));
    }
}
