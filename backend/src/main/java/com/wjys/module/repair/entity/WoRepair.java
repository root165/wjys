package com.wjys.module.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wo_repair")
public class WoRepair {
    @TableId(type = IdType.AUTO) private Long id;
    private Long equipId;
    private String sourceType;
    private Long sourceId;
    private String description;
    private Long reporterId;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
