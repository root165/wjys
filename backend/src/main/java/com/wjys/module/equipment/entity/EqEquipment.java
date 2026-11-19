package com.wjys.module.equipment.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("eq_equipment")
public class EqEquipment {
    @TableId(type = IdType.AUTO) private Long id;
    private Long categoryId;
    private String equipCode;
    private String equipName;
    private String model;
    private String manufacturer;
    private String location;
    private Integer status;
    private Long responsibleUserId;
    private LocalDate purchaseDate;
    private LocalDate warrantyExpire;
    private String description;
    private Integer deleted;
    private Integer version;
}
