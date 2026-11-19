package com.wjys.module.equipment.vo;

import java.time.LocalDate;
import java.util.List;

public record EquipmentDetailVO(Long id, Long categoryId, String equipCode, String equipName, String model, String manufacturer, String location, Integer status, String statusLabel, Long responsibleUserId, LocalDate purchaseDate, LocalDate warrantyExpire, String description, List<ParamVO> params, List<StatusLogVO> statusLogs) { }
