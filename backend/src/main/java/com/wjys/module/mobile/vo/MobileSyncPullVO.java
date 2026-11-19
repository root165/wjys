package com.wjys.module.mobile.vo;

import com.wjys.module.inspection.vo.TaskVO;
import com.wjys.support.message.MessageVO;

import java.util.List;

public record MobileSyncPullVO(String cursor, List<TaskVO> tasks, List<MessageVO> messages) { }
