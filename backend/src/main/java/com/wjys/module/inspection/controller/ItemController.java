package com.wjys.module.inspection.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.module.inspection.dto.CreateItemRequest;
import com.wjys.module.inspection.service.ItemService;
import com.wjys.module.inspection.vo.ItemVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/inspection/items")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('inspection:config:view')")
public class ItemController {
    private final ItemService itemService;
    @GetMapping public ApiResponse<List<ItemVO>> list(@RequestParam(required = false) Long pointId) { return ApiResponse.success(itemService.list(pointId)); }
    @PostMapping @PreAuthorize("hasAuthority('inspection:config:write') or hasRole('system_admin')") @OperLog("创建巡检检查项") public ApiResponse<ItemVO> create(@Valid @RequestBody CreateItemRequest request) { return ApiResponse.success("检查项创建成功", itemService.create(request)); }
}
