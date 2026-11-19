package com.wjys.module.repair.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.repair.service.RepairService;
import com.wjys.module.repair.dto.RejectRequest;
import com.wjys.module.repair.vo.RepairVO;
import com.wjys.module.repair.vo.WorkOrderVO;
import com.wjys.security.WjysPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/repairs")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('repair:view')")
public class RepairController {
    private final RepairService repairService;

    @GetMapping
    public ApiResponse<List<RepairVO>> list(@RequestParam(required = false) Integer status) { return ApiResponse.success(repairService.list(status)); }

    @PostMapping("/from-abnormal/{abnormalId}")
    @PreAuthorize("hasAuthority('repair:create') or hasRole('system_admin')")
    @OperLog("异常转报修")
    public ApiResponse<RepairVO> createFromAbnormal(@PathVariable Long abnormalId, @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(repairService.createFromAbnormal(abnormalId, principal.id()));
    }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('repair:manage') or hasRole('system_admin')")
    @OperLog("受理报修")
    public ApiResponse<WorkOrderVO> accept(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(repairService.accept(id, principal.id())); }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('repair:manage') or hasRole('system_admin')")
    @OperLog("驳回报修")
    public ApiResponse<Void> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request, @AuthenticationPrincipal WjysPrincipal principal) { repairService.reject(id, request, principal.id()); return ApiResponse.success(null); }
}
