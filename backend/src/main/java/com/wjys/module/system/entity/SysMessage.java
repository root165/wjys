package com.wjys.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_message")
public class SysMessage {
    @TableId(type = IdType.AUTO) private Long id;
    private Long userId;
    private String messageType;
    private String title;
    private String content;
    private String bizType;
    private Long bizId;
    private Integer readFlag;
    private Integer pushStatus;
    private Integer retryCount;
    private LocalDateTime createdTime;
    private LocalDateTime readTime;
}
