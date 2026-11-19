package com.wjys.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("eq_param")
public class EqParam {
    @TableId(type = IdType.AUTO) private Long id;
    private Long equipId;
    private String paramName;
    private String paramValue;
    private String unit;
    private Integer sortNo;
}
