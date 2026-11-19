package com.wjys.support.file;

import com.wjys.module.system.entity.SysFile;
import com.wjys.module.system.mapper.SysFileMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTests {
    @Mock private SysFileMapper fileMapper;
    @Mock private ObjectStorageService storageService;
    @InjectMocks private FileCleanupService fileCleanupService;

    @Test
    void deletesExpiredUnboundObjectAndRow() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 17, 12, 0);
        SysFile file = file(7L, "2026/11/17/expired.jpg", now.minusHours(1));
        when(fileMapper.selectList(any())).thenReturn(List.of(file));
        when(fileMapper.delete(any())).thenReturn(1);

        assertEquals(1, fileCleanupService.cleanupExpiredUnboundFiles(now));

        verify(storageService).delete(file.getObjectKey());
        verify(fileMapper).delete(any());
    }

    @Test
    void keepsRowForRetryWhenObjectDeleteFails() {
        LocalDateTime now = LocalDateTime.of(2026, 11, 17, 12, 0);
        SysFile file = file(8L, "2026/11/17/retry.jpg", now.minusHours(1));
        when(fileMapper.selectList(any())).thenReturn(List.of(file));
        org.mockito.Mockito.doThrow(new IllegalStateException("storage unavailable"))
                .when(storageService).delete(file.getObjectKey());

        assertEquals(0, fileCleanupService.cleanupExpiredUnboundFiles(now));

        verify(fileMapper, never()).delete(any());
    }

    private SysFile file(Long id, String objectKey, LocalDateTime expireTime) {
        SysFile file = new SysFile();
        file.setId(id);
        file.setObjectKey(objectKey);
        file.setBindStatus(0);
        file.setExpireTime(expireTime);
        return file;
    }
}
