package com.wjys.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("eq_category")
public class EqCategory {
    @TableId(type = IdType.AUTO) private Long id;
    private Long parentId;
    private String categoryName;
    private Integer sortNo;
    private Integer status;
}
