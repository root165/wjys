package com.wjys.support.file;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.wjys.common.exception.BusinessException;
import com.wjys.module.system.entity.SysFile;
import com.wjys.module.system.mapper.SysFileMapper;
import com.wjys.support.file.vo.FileUploadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class FileService {
    private static final long MAX_PHOTO_BYTES = 20 * 1024 * 1024;
    private final SysFileMapper fileMapper;
    private final ObjectStorageService storageService;

    @Transactional
    public FileUploadVO upload(MultipartFile file) {
        if (file == null || file.isEmpty()) throw new BusinessException(HttpStatus.BAD_REQUEST, "上传文件不能为空");
        if (file.getSize() > MAX_PHOTO_BYTES) throw new BusinessException(HttpStatus.BAD_REQUEST, "图片不能超过 20 MB");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) throw new BusinessException(HttpStatus.BAD_REQUEST, "只允许上传图片");
        try {
            byte[] content = file.getBytes();
            String checksum = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(content));
            String extension = extension(file.getOriginalFilename(), contentType);
            String objectKey = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy/MM/dd")) + "/" + UUID.randomUUID() + extension;
            storageService.store(objectKey, content, contentType);
            SysFile entity = new SysFile(); entity.setObjectKey(objectKey); entity.setUploadToken(UUID.randomUUID().toString()); entity.setOriginalName(file.getOriginalFilename()); entity.setChecksum(checksum); entity.setBindStatus(0); entity.setExpireTime(LocalDateTime.now().plusHours(24)); fileMapper.insert(entity);
            return new FileUploadVO(entity.getId(), entity.getUploadToken(), checksum, entity.getOriginalName());
        } catch (BusinessException exception) { throw exception; } catch (Exception exception) { throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "文件上传失败"); }
    }

    @Transactional
    public void bindAll(List<Long> fileIds, String bizType, Long bizId) {
        List<SysFile> files = fileMapper.selectBatchIds(fileIds);
        if (files.size() != fileIds.stream().distinct().count()) throw new BusinessException(HttpStatus.BAD_REQUEST, "存在无效文件");
        for (SysFile file : files) {
            if (Integer.valueOf(1).equals(file.getBindStatus()) && !(bizType.equals(file.getBizType()) && bizId.equals(file.getBizId()))) throw new BusinessException(HttpStatus.CONFLICT, "文件已绑定其他业务");
            if (Integer.valueOf(1).equals(file.getBindStatus())) continue;
            fileMapper.update(null, new LambdaUpdateWrapper<SysFile>().eq(SysFile::getId, file.getId()).eq(SysFile::getBindStatus, 0).set(SysFile::getBindStatus, 1).set(SysFile::getBizType, bizType).set(SysFile::getBizId, bizId).set(SysFile::getExpireTime, (LocalDateTime) null));
        }
    }

    public byte[] read(Long fileId) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在");
        if (!Integer.valueOf(1).equals(file.getBindStatus())) throw new BusinessException(HttpStatus.FORBIDDEN, "文件尚未绑定业务");
        return storageService.read(file.getObjectKey());
    }

    public String contentType(Long fileId) {
        SysFile file = fileMapper.selectById(fileId);
        if (file == null) throw new BusinessException(HttpStatus.NOT_FOUND, "文件不存在");
        if (!Integer.valueOf(1).equals(file.getBindStatus())) throw new BusinessException(HttpStatus.FORBIDDEN, "文件尚未绑定业务");
        String name = file.getOriginalName() == null ? "" : file.getOriginalName().toLowerCase(Locale.ROOT);
        if (name.endsWith(".png")) return "image/png";
        if (name.endsWith(".webp")) return "image/webp";
        if (name.endsWith(".gif")) return "image/gif";
        return "image/jpeg";
    }

    private String extension(String originalName, String contentType) {
        if (originalName != null && originalName.contains(".")) return originalName.substring(originalName.lastIndexOf('.')).toLowerCase(Locale.ROOT);
        return switch (contentType) { case "image/png" -> ".png"; case "image/webp" -> ".webp"; default -> ".jpg"; };
    }
}
