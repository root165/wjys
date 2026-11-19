package com.wjys.module.mobile.service;

import com.wjys.common.exception.BusinessException;
import com.wjys.module.inspection.service.TaskService;
import com.wjys.module.inspection.service.TaskSubmitService;
import com.wjys.module.inspection.vo.TaskSubmitVO;
import com.wjys.module.inspection.vo.TaskVO;
import com.wjys.module.mobile.dto.MobileSyncPushItem;
import com.wjys.module.mobile.dto.MobileSyncPushRequest;
import com.wjys.module.mobile.vo.MobileSyncPullVO;
import com.wjys.module.mobile.vo.MobileSyncPushResult;
import com.wjys.module.mobile.vo.MobileSyncPushVO;
import com.wjys.support.message.MessageService;
import com.wjys.support.message.MessageVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MobileSyncService {
    private final TaskService taskService;
    private final TaskSubmitService taskSubmitService;
    private final MessageService messageService;

    public MobileSyncPullVO pull(Long userId, String cursorValue, String lastSyncTime, String modules) {
        Cursor cursor = parseCursor(cursorValue, lastSyncTime);
        List<String> requested = modules == null || modules.isBlank()
                ? List.of("inspection", "messages")
                : Arrays.stream(modules.split(",")).map(String::trim).filter(item -> !item.isBlank()).toList();
        List<TaskVO> tasks = requested.contains("inspection") ? taskService.assignedTasks(userId) : List.of();
        List<MessageVO> messages = requested.contains("messages")
                ? (cursor == null ? messageService.listForSync(userId) : messageService.listSince(userId, cursor.time(), cursor.id())) : List.of();
        return new MobileSyncPullVO(nextCursor(cursor, messages), tasks, messages);
    }

    public MobileSyncPushVO push(MobileSyncPushRequest request, Long userId) {
        List<MobileSyncPushResult> results = request.items().stream().map(item -> pushOne(item, userId)).toList();
        return new MobileSyncPushVO(results);
    }

    private MobileSyncPushResult pushOne(MobileSyncPushItem item, Long userId) {
        String submitNo = item.payload().submitNo();
        try {
            TaskSubmitVO result = taskSubmitService.submit(item.taskId(), item.payload(), userId);
            return new MobileSyncPushResult(submitNo, "confirmed", result.taskId(), null, result.idempotentReplay() ? "重复提交已幂等返回" : "同步成功");
        } catch (BusinessException exception) {
            String status = exception.getStatus() == HttpStatus.CONFLICT ? "conflict" : "rejected";
            return new MobileSyncPushResult(submitNo, status, item.taskId(), String.valueOf(exception.getStatus().value()), exception.getMessage());
        } catch (RuntimeException exception) {
            return new MobileSyncPushResult(submitNo, "retry", item.taskId(), "SERVER_ERROR", "服务暂不可用，请稍后重试");
        }
    }

    private Cursor parseCursor(String cursorValue, String legacyTime) {
        if (cursorValue != null && !cursorValue.isBlank()) {
            try {
                String decoded = new String(Base64.getUrlDecoder().decode(cursorValue), StandardCharsets.UTF_8);
                String[] parts = decoded.split("\\|", 2);
                return new Cursor(LocalDateTime.parse(parts[0]), Long.parseLong(parts[1]));
            } catch (RuntimeException exception) { throw new BusinessException(HttpStatus.BAD_REQUEST, "cursor 格式无效"); }
        }
        if (legacyTime == null || legacyTime.isBlank()) return null;
        try { return new Cursor(LocalDateTime.parse(legacyTime), 0L); } catch (RuntimeException exception) { throw new BusinessException(HttpStatus.BAD_REQUEST, "lastSyncTime 必须是 ISO-8601 本地时间"); }
    }

    private String nextCursor(Cursor current, List<MessageVO> messages) {
        Cursor next = current;
        if (!messages.isEmpty()) {
            MessageVO last = messages.get(messages.size() - 1);
            next = new Cursor(last.createdTime(), last.id());
        }
        if (next == null) next = new Cursor(LocalDateTime.now(), 0L);
        return Base64.getUrlEncoder().withoutPadding().encodeToString((next.time() + "|" + next.id()).getBytes(StandardCharsets.UTF_8));
    }

    private record Cursor(LocalDateTime time, Long id) { }
}
