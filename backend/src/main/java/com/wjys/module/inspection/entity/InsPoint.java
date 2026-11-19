package com.wjys.module.inspection.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("ins_point")
public class InsPoint {
    @TableId(type = IdType.AUTO) private Long id;
    private Long equipId;
    private String pointCode;
    private String pointName;
    private String location;
    private Integer status;
}
