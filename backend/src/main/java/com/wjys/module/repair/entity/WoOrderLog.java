package com.wjys.module.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("wo_order_log")
public class WoOrderLog {
    @TableId(type = IdType.AUTO) private Long id;
    private Long orderId;
    private String action;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdTime;
}
