package com.wjys.module.spare.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.spare.dto.ConsumeSparesRequest;
import com.wjys.module.spare.service.SpareConsumptionService;
import com.wjys.module.spare.vo.SpareRecordVO;
import com.wjys.security.WjysPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/work-orders")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('spare:view')")
public class SpareConsumptionController {
    private final SpareConsumptionService consumptionService;

    @GetMapping("/{orderId}/spares/records")
    public ApiResponse<List<SpareRecordVO>> records(@PathVariable Long orderId) { return ApiResponse.success(consumptionService.records(orderId)); }

    @PostMapping("/{orderId}/spares/consume")
    @PreAuthorize("hasAuthority('spare:consume') or hasRole('system_admin')")
    @OperLog("工单领用备件")
    public ApiResponse<List<SpareRecordVO>> consume(@PathVariable Long orderId, @Valid @RequestBody ConsumeSparesRequest request, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(consumptionService.consume(orderId, request.items(), principal.id())); }

    @PostMapping("/spare-records/{recordId}/reverse")
    @PreAuthorize("hasAuthority('spare:consume') or hasRole('system_admin')")
    @OperLog("撤销备件领用")
    public ApiResponse<SpareRecordVO> reverse(@PathVariable Long recordId, @RequestBody(required = false) java.util.Map<String, String> body, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(consumptionService.reverse(recordId, principal.id(), body == null ? null : body.get("remark"))); }
}
