package com.wjys.module.maintenance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mt_plan")
public class MtPlan {
    @TableId(type = IdType.AUTO) private Long id;
    private String planCode;
    private String planName;
    private Long equipId;
    private Integer maintainType;
    private Integer cycleType;
    private Integer cycleValue;
    private String content;
    private Long assigneeId;
    private LocalDate nextDate;
    private Integer status;
    private Long creatorId;
    private Integer version;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
