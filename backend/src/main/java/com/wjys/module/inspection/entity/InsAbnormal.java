package com.wjys.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ins_abnormal")
public class InsAbnormal {
    @TableId(type = IdType.AUTO) private Long id;
    private Long taskId;
    private Long taskItemId;
    private Long equipId;
    private Long pointId;
    private Integer severity;
    private String description;
    private Integer status;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
