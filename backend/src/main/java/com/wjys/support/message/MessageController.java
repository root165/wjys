package com.wjys.support.message;

import com.wjys.common.model.ApiResponse;
import com.wjys.security.WjysPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/messages")
@RequiredArgsConstructor
@PreAuthorize("hasAuthority('message:view')")
public class MessageController {
    private final MessageService messageService;

    @GetMapping
    public ApiResponse<List<MessageVO>> list(@RequestParam(required = false) Integer readFlag,
                                              @AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(messageService.list(principal.id(), readFlag));
    }

    @GetMapping("/unread-count")
    public ApiResponse<Long> unreadCount(@AuthenticationPrincipal WjysPrincipal principal) {
        return ApiResponse.success(messageService.unreadCount(principal.id()));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<Void> markRead(@PathVariable Long id, @AuthenticationPrincipal WjysPrincipal principal) {
        messageService.markRead(id, principal.id()); return ApiResponse.success(null);
    }
}
