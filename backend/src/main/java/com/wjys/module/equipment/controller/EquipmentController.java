package com.wjys.module.equipment.controller;

import com.wjys.common.annotation.OperLog;
import com.wjys.common.model.ApiResponse;
import com.wjys.common.model.PageResult;
import com.wjys.module.equipment.dto.ChangeEquipmentStatusRequest;
import com.wjys.module.equipment.dto.EquipmentQuery;
import com.wjys.module.equipment.dto.SaveEquipmentRequest;
import com.wjys.module.equipment.dto.SaveParamRequest;
import com.wjys.module.equipment.service.EquipmentQrService;
import com.wjys.module.equipment.service.EquipmentService;
import com.wjys.module.equipment.service.EquipmentStatusService;
import com.wjys.module.equipment.vo.EquipmentDetailVO;
import com.wjys.module.equipment.vo.EquipmentListVO;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/equipment/equipments")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('equipment:view')")
public class EquipmentController {
    private final EquipmentService equipmentService;
    private final EquipmentStatusService statusService;
    private final EquipmentQrService qrService;

    @GetMapping
    public ApiResponse<PageResult<EquipmentListVO>> page(@RequestParam(required = false) String keyword, @RequestParam(required = false) Long categoryId, @RequestParam(required = false) Integer status, @RequestParam(defaultValue = "1") long pageNum, @RequestParam(defaultValue = "20") long pageSize) {
        return ApiResponse.success(equipmentService.page(new EquipmentQuery(keyword, categoryId, status, pageNum, pageSize)));
    }

    @GetMapping("/{id}")
    public ApiResponse<EquipmentDetailVO> detail(@PathVariable Long id) { return ApiResponse.success(equipmentService.detail(id)); }

    @PostMapping
    @PreAuthorize("hasAuthority('equipment:write') or hasRole('system_admin')")
    @OperLog("创建设备")
    public ApiResponse<EquipmentListVO> create(@Valid @RequestBody SaveEquipmentRequest request) { return ApiResponse.success("设备创建成功", equipmentService.create(request)); }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('equipment:write') or hasRole('system_admin')")
    @OperLog("更新设备")
    public ApiResponse<EquipmentListVO> update(@PathVariable Long id, @Valid @RequestBody SaveEquipmentRequest request) { return ApiResponse.success(equipmentService.update(id, request)); }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('equipment:write') or hasRole('system_admin')")
    @OperLog("删除设备")
    public ApiResponse<Void> delete(@PathVariable Long id) { equipmentService.delete(id); return ApiResponse.success(null); }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('equipment:status') or hasRole('system_admin')")
    @OperLog("变更设备状态")
    public ApiResponse<Void> changeStatus(@PathVariable Long id, @Valid @RequestBody ChangeEquipmentStatusRequest request) { statusService.changeStatus(id, request); return ApiResponse.success(null); }

    @PutMapping("/{id}/params/{paramId}")
    @PreAuthorize("hasAuthority('equipment:write') or hasRole('system_admin')")
    @OperLog("编辑设备参数")
    public ApiResponse<com.wjys.module.equipment.vo.ParamVO> updateParam(@PathVariable Long id, @PathVariable Long paramId, @Valid @RequestBody SaveParamRequest request) { return ApiResponse.success(equipmentService.updateParam(id, paramId, request)); }

    @GetMapping(value = "/{id}/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> qrcode(@PathVariable Long id) { return ResponseEntity.ok().cacheControl(CacheControl.noStore()).contentType(MediaType.IMAGE_PNG).body(qrService.generate(id)); }
}
