package com.wjys.module.spare.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("wo_spare_record")
public class WoSpareRecord {
    @TableId(type = IdType.AUTO) private Long id;
    private Long orderId;
    private Long spareId;
    private Long sourceRecordId;
    private String direction;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private Long operatorId;
    private String remark;
    private LocalDateTime createdTime;
}
