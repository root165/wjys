package com.wjys.module.dashboard.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("stat_equip_daily")
public class StatEquipDaily {
    @TableId(type = IdType.AUTO) private Long id;
    private LocalDate statDate;
    private Long equipId;
    private Integer taskTotal;
    private Integer taskFinish;
    private Integer abnormalCount;
    private Integer faultCount;
    private Integer downtimeMinutes;
    private BigDecimal mtbfHours;
    private BigDecimal mttrMinutes;
    private BigDecimal healthScore;
}
