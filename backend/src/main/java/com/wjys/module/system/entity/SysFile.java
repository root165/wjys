package com.wjys.module.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_file")
public class SysFile {
    @TableId(type = IdType.AUTO) private Long id;
    private String objectKey;
    private String uploadToken;
    private String originalName;
    private String checksum;
    private Integer bindStatus;
    private String bizType;
    private Long bizId;
    private LocalDateTime expireTime;
    private LocalDateTime createdTime;
}
