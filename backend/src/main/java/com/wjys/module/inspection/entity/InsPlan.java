package com.wjys.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@TableName("ins_plan")
public class InsPlan {
    @TableId(type = IdType.AUTO) private Long id;
    private String planCode;
    private String planName;
    private String cycleType;
    private String cycleValue;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalTime executeTime;
    private String assignmentMode;
    private Long assignmentDeptId;
    private Long responsibleUserId;
    private Integer status;
}
