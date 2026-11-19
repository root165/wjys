package com.wjys.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@TableName("ins_task")
public class InsTask {
    @TableId(type = IdType.AUTO) private Long id;
    private String taskNo;
    private Long planId;
    private LocalDate planTime;
    private Long assigneeId;
    private Integer status;
    private String submitNo;
    private String submitHash;
    private LocalDateTime clientSubmitTime;
    private Integer offlineFlag;
    private LocalDateTime finishTime;
    private Integer totalItems;
    private Integer abnormalCount;
}
