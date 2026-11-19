package com.wjys.module.spare.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.spare.dto.AdjustStockRequest;
import com.wjys.module.spare.dto.SaveSpareRequest;
import com.wjys.module.spare.service.SpareService;
import com.wjys.module.spare.vo.SpareVO;
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
@RequestMapping("/api/spares")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('spare:view')")
public class SpareController {
    private final SpareService spareService;

    @GetMapping
    public ApiResponse<List<SpareVO>> list(@RequestParam(required = false) String keyword, @RequestParam(required = false) Integer status) { return ApiResponse.success(spareService.list(keyword, status, false)); }

    @GetMapping("/alerts")
    public ApiResponse<List<SpareVO>> alerts() { return ApiResponse.success(spareService.list(null, 1, true)); }

    @PostMapping
    @PreAuthorize("hasAuthority('spare:write') or hasRole('system_admin')")
    @OperLog("创建备件")
    public ApiResponse<SpareVO> create(@Valid @RequestBody SaveSpareRequest request) { return ApiResponse.success(spareService.create(request)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('spare:write') or hasRole('system_admin')")
    @OperLog("更新备件")
    public ApiResponse<SpareVO> update(@PathVariable Long id, @Valid @RequestBody SaveSpareRequest request) { return ApiResponse.success(spareService.update(id, request)); }

    @PutMapping("/{id}/stock")
    @PreAuthorize("hasAuthority('spare:write') or hasRole('system_admin')")
    @OperLog("调整备件库存")
    public ApiResponse<SpareVO> adjustStock(@PathVariable Long id, @Valid @RequestBody AdjustStockRequest request, @AuthenticationPrincipal WjysPrincipal principal) { return ApiResponse.success(spareService.adjustStock(id, request, principal.id())); }
}
