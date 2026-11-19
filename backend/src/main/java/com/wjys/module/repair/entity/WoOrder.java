package com.wjys.module.repair.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

@Data
@TableName("wo_order")
public class WoOrder {
    @TableId(type = IdType.AUTO) private Long id;
    private Long repairId;
    private String orderNo;
    private Long assigneeId;
    private Integer status;
    private String dispatchRemark;
    private LocalDateTime expectFinishTime;
    private String finishReason;
    private String repairPlan;
    private String finishResult;
    private Integer downtimeMinutes;
    private String acceptImages;
    private String rejectReason;
    private LocalDateTime finishedTime;
    private LocalDateTime acceptedTime;
    private Integer version;
    private BigDecimal spareCost;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
