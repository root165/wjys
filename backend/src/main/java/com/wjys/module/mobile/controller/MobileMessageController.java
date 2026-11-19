package com.wjys.module.mobile.controller;

import com.wjys.common.model.ApiResponse;
import com.wjys.security.WjysPrincipal;
import com.wjys.support.message.MessageService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mobile/messages")
@RequiredArgsConstructor
@PreAuthorize("hasRole('inspector')")
public class MobileMessageController {
    private final MessageService messageService;

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) {
        messageService.markRead(id, principal.id());
        return ApiResponse.success(null);
    }
}
