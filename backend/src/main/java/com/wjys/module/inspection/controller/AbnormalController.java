package com.wjys.module.inspection.controller;

import com.wjys.common.model.ApiResponse;
import com.wjys.module.inspection.service.AbnormalService;
import com.wjys.module.inspection.vo.AbnormalVO;
import com.wjys.module.repair.service.RepairService;
import com.wjys.module.repair.vo.RepairVO;
import com.wjys.security.WjysPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inspection/abnormals")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inspection:abnormal:view')")
public class AbnormalController {
    private final AbnormalService abnormalService;
    private final RepairService repairService;
    @GetMapping public ApiResponse<List<AbnormalVO>> list(@RequestParam(required = false) Integer status) { return ApiResponse.success(abnormalService.list(status)); }
    @PostMapping("/{id}/to-repair")
    @PreAuthorize("hasAuthority('repair:create') or hasRole('system_admin')")
    public ApiResponse<RepairVO> toRepair(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(repairService.createFromAbnormal(id, principal.id())); }
}
