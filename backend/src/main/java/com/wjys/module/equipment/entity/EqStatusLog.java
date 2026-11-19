package com.wjys.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("eq_status_log")
public class EqStatusLog {
    @TableId(type = IdType.AUTO) private Long id;
    private Long equipId;
    private Integer fromStatus;
    private Integer toStatus;
    private String sourceType;
    private Long sourceId;
    private String reason;
    private LocalDateTime createdTime;
}
