package com.wjys.module.mobile.controller;

import com.wjys.common.model.ApiResponse;
import com.wjys.module.mobile.dto.MobileSyncPushRequest;
import com.wjys.module.mobile.service.MobileSyncService;
import com.wjys.module.mobile.vo.MobileSyncPullVO;
import com.wjys.module.mobile.vo.MobileSyncPushVO;
import com.wjys.security.WjysPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/sync")
@RequiredArgsConstructor
@PreAuthorize("hasRole('inspector')")
public class MobileSyncController {
    private final MobileSyncService mobileSyncService;

    @GetMapping("/pull")
    public ResponseEntity<ApiResponse<MobileSyncPullVO>> pull(@RequestParam(required = false) String cursor,
                                               @RequestParam(required = false) String lastSyncTime,
                                               @RequestParam(required = false) String modules,
                                               @AuthenticationPrincipal WjysPrincipal principal) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache").body(ApiResponse.success(mobileSyncService.pull(principal.id(), cursor, lastSyncTime, modules)));
    }

    @PostMapping("/push")
    public ApiResponse<MobileSyncPushVO> push(@Valid @RequestBody MobileSyncPushRequest request,
                                               @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(mobileSyncService.push(request, principal.id()));
    }
}
