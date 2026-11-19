package com.wjys.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("ins_task_item")
public class InsTaskItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long taskId;
    private Long pointId;
    private Long itemId;
    private Long equipId;
    private String equipCode;
    private String pointCode;
    private String itemCode;
    private String itemName;
    private String valueType;
    private String checkMethod;
    private String unit;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String optionsJson;
    private Integer requiredPhoto;
    private Integer checkResult;
    private String checkValue;
    private String remark;
    private String photoFileIds;
    private LocalDateTime checkTime;
    private String abnormalReason;
}
