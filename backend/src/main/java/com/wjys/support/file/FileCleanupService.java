package com.wjys.support.file;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.module.system.entity.SysFile;
import com.wjys.module.system.mapper.SysFileMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileCleanupService {
    private static final int BATCH_SIZE = 100;

    private final SysFileMapper fileMapper;
    private final ObjectStorageService storageService;

    public int cleanupExpiredUnboundFiles(LocalDateTime now) {
        List<SysFile> files = fileMapper.selectList(new LambdaQueryWrapper<SysFile>()
                .eq(SysFile::getBindStatus, 0)
                .isNotNull(SysFile::getExpireTime)
                .le(SysFile::getExpireTime, now)
                .orderByAsc(SysFile::getExpireTime)
                .last("limit " + BATCH_SIZE));
        int deleted = 0;
        for (SysFile file : files) {
            try {
                storageService.delete(file.getObjectKey());
                int affected = fileMapper.delete(new LambdaUpdateWrapper<SysFile>()
                        .eq(SysFile::getId, file.getId())
                        .eq(SysFile::getBindStatus, 0)
                        .le(SysFile::getExpireTime, now));
                if (affected == 1) deleted++;
            } catch (RuntimeException exception) {
                log.warn("过期未绑定文件清理失败，fileId={}, objectKey={}", file.getId(), file.getObjectKey(), exception);
            }
        }
        return deleted;
    }
}
