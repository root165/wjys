package com.wjys.support.message;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.system.entity.SysMessage;
import com.wjys.module.system.mapper.SysMessageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {
    private final SysMessageMapper messageMapper;

    public List<MessageVO> list(Long userId, Integer readFlag) {
        return messageMapper.selectList(new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, userId)
                        .eq(readFlag != null, SysMessage::getReadFlag, readFlag)
                        .orderByDesc(SysMessage::getCreatedTime)
                        .orderByDesc(SysMessage::getId))
                .stream().map(this::toVO).toList();
    }

    public List<MessageVO> listSince(Long userId, LocalDateTime since) {
        return listSince(userId, since, 0L);
    }

    public List<MessageVO> listSince(Long userId, LocalDateTime since, Long afterId) {
        return messageMapper.selectList(new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, userId)
                        .and(wrapper -> wrapper.gt(SysMessage::getCreatedTime, since)
                                .or(inner -> inner.eq(SysMessage::getCreatedTime, since).gt(SysMessage::getId, afterId)))
                        .orderByAsc(SysMessage::getCreatedTime)
                        .orderByAsc(SysMessage::getId))
                .stream().map(this::toVO).toList();
    }

    public List<MessageVO> listForSync(Long userId) {
        return messageMapper.selectList(new LambdaQueryWrapper<SysMessage>()
                        .eq(SysMessage::getUserId, userId)
                        .orderByAsc(SysMessage::getCreatedTime)
                        .orderByAsc(SysMessage::getId))
                .stream().map(this::toVO).toList();
    }

    public long unreadCount(Long userId) {
        return messageMapper.selectCount(new LambdaQueryWrapper<SysMessage>().eq(SysMessage::getUserId, userId).eq(SysMessage::getReadFlag, 0));
    }

    @Transactional
    public void markRead(Long id, Long userId) {
        SysMessage message = messageMapper.selectOne(new LambdaQueryWrapper<SysMessage>().eq(SysMessage::getId, id).eq(SysMessage::getUserId, userId));
        if (message == null) throw new BusinessException(HttpStatus.NOT_FOUND, "消息不存在");
        if (Integer.valueOf(1).equals(message.getReadFlag())) return;
        int affected = messageMapper.update(null, new LambdaUpdateWrapper<SysMessage>().eq(SysMessage::getId, id).eq(SysMessage::getUserId, userId).eq(SysMessage::getReadFlag, 0).set(SysMessage::getReadFlag, 1).set(SysMessage::getReadTime, LocalDateTime.now()));
        if (affected != 1) throw new BusinessException(HttpStatus.CONFLICT, "消息状态已被其他操作更新");
    }

    @Transactional
    public SysMessage create(Long userId, String messageType, String title, String content, String bizType, Long bizId) {
        SysMessage message = new SysMessage(); message.setUserId(userId); message.setMessageType(messageType); message.setTitle(title); message.setContent(content); message.setBizType(bizType); message.setBizId(bizId); message.setReadFlag(0); message.setPushStatus(0); message.setRetryCount(0); messageMapper.insert(message); return message;
    }

    private MessageVO toVO(SysMessage message) { return new MessageVO(message.getId(), message.getMessageType(), message.getTitle(), message.getContent(), message.getBizType(), message.getBizId(), message.getReadFlag(), message.getCreatedTime(), message.getReadTime()); }
}
