package com.wjys.module.maintenance.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("mt_record")
public class MtRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private String recordCode;
    private Long planId;
    private Long equipId;
    private LocalDate maintainDate;
    private Long maintainerId;
    private String maintainContent;
    private Integer maintainResult;
    private BigDecimal cost;
    private Integer downtimeMinutes;
    private String images;
    private Integer status;
    private Long auditUser;
    private LocalDateTime auditTime;
    private String remark;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
