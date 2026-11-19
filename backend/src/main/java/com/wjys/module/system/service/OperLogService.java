package com.wjys.module.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wjys.common.model.PageResult;
import com.wjys.module.system.entity.SysOperLog;
import com.wjys.module.system.mapper.SysOperLogMapper;
import com.wjys.module.system.vo.OperLogVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OperLogService {
    private final SysOperLogMapper logMapper;

    public void record(Long userId, String username, String operation, String method, String uri, boolean success, String traceId) {
        SysOperLog log = new SysOperLog();
        log.setUserId(userId); log.setUsername(username); log.setOperation(operation); log.setRequestMethod(method); log.setRequestUri(uri); log.setSuccess(success ? 1 : 0); log.setTraceId(traceId);
        logMapper.insert(log);
    }

    public PageResult<OperLogVO> page(long pageNum, long pageSize) {
        Page<SysOperLog> page = logMapper.selectPage(new Page<>(pageNum, Math.min(pageSize, 100)), null);
        return new PageResult<>(page.getTotal(), pageNum, pageSize, page.getRecords().stream().map(log -> new OperLogVO(log.getId(), log.getUserId(), log.getUsername(), log.getOperation(), log.getRequestMethod(), log.getRequestUri(), log.getSuccess(), log.getTraceId(), log.getCreatedTime())).toList());
    }
}
