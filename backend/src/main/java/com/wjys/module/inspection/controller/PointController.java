package com.wjys.module.inspection.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.inspection.dto.CreatePointRequest;
import com.wjys.module.inspection.service.PointService;
import com.wjys.module.inspection.vo.PointVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inspection/points")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inspection:config:view')")
public class PointController {
    private final PointService pointService;
    @GetMapping public ApiResponse<List<PointVO>> list() { return ApiResponse.success(pointService.list()); }
    @PostMapping @PreAuthorize("hasAuthority('inspection:config:write') or hasRole('system_admin')") @OperLog("创建巡检点位") public ApiResponse<PointVO> create(@Valid @RequestBody CreatePointRequest request) { return ApiResponse.success("巡检点位创建成功", pointService.create(request)); }
}
