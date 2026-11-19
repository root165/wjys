package com.wjys.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("ins_item")
public class InsItem {
    @TableId(type = IdType.AUTO) private Long id;
    private Long pointId;
    private String itemCode;
    private String itemName;
    private String checkMethod;
    private String valueType;
    private BigDecimal minValue;
    private BigDecimal maxValue;
    private String unit;
    private String optionsJson;
    private Integer requiredPhoto;
    private Integer status;
}
