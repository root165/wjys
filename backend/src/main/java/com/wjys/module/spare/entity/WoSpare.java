package com.wjys.module.spare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wo_spare")
public class WoSpare {
    @TableId(type = IdType.AUTO) private Long id;
    private String spareCode;
    private String spareName;
    private String specification;
    private String unit;
    private Integer stockQty;
    private Integer warningQty;
    private BigDecimal unitPrice;
    private Integer status;
    private Integer version;
    private LocalDateTime createdTime;
    private LocalDateTime updatedTime;
}
