package com.wjys.module.repair.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.repair.dto.AcceptRequest;
import com.wjys.module.repair.dto.DispatchRequest;
import com.wjys.module.repair.dto.FinishRequest;
import com.wjys.module.repair.dto.RejectRequest;
import com.wjys.module.repair.service.WorkOrderService;
import com.wjys.module.repair.vo.OrderLogVO;
import com.wjys.module.repair.vo.WorkOrderVO;
import com.wjys.security.WjysPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('repair:view')")
public class WorkOrderController {
    private final WorkOrderService workOrderService;

    @GetMapping
    public ApiResponse<List<WorkOrderVO>> list(@RequestParam(required = false) Integer status) { return ApiResponse.success(workOrderService.list(status)); }

    @GetMapping("/{id}")
    public ApiResponse<WorkOrderVO> detail(@PathVariable Long id) { return ApiResponse.success(workOrderService.detail(id)); }

    @GetMapping("/{id}/logs")
    public ApiResponse<List<OrderLogVO>> logs(@PathVariable Long id) { return ApiResponse.success(workOrderService.logs(id)); }

    @PutMapping("/{id}/dispatch")
    @PreAuthorize("hasAuthority('repair:manage') or hasRole('system_admin')")
    @OperLog("工单派工")
    public ApiResponse<WorkOrderVO> dispatch(@PathVariable Long id, @Valid @RequestBody DispatchRequest request, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(workOrderService.dispatch(id, request, principal.id())); }

    @PutMapping("/{id}/start")
    @PreAuthorize("hasAuthority('repair:operate') or hasRole('system_admin')")
    @OperLog("工单接单")
    public ApiResponse<WorkOrderVO> start(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(workOrderService.start(id, principal.id())); }

    @PutMapping("/{id}/finish")
    @PreAuthorize("hasAuthority('repair:operate') or hasRole('system_admin')")
    @OperLog("工单完工")
    public ApiResponse<WorkOrderVO> finish(@PathVariable Long id, @Valid @RequestBody FinishRequest request, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(workOrderService.finish(id, request, principal.id())); }

    @PutMapping("/{id}/accept")
    @PreAuthorize("hasAuthority('repair:accept') or hasRole('system_admin')")
    @OperLog("工单验收")
    public ApiResponse<WorkOrderVO> accept(@PathVariable Long id, @RequestBody(required = false) AcceptRequest request, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(workOrderService.accept(id, principal.id(), request == null ? null : request.remark())); }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('repair:accept') or hasRole('system_admin')")
    @OperLog("工单验收退回")
    public ApiResponse<WorkOrderVO> reject(@PathVariable Long id, @Valid @RequestBody RejectRequest request, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(workOrderService.reject(id, request.reason(), principal.id())); }
}
