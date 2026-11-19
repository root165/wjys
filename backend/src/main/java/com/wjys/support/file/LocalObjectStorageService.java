package com.wjys.support.file;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
@ConditionalOnProperty(name = "wjys.file.minio-enabled", havingValue = "false", matchIfMissing = true)
public class LocalObjectStorageService implements ObjectStorageService {
    private final Path root;

    public LocalObjectStorageService(@Value("${wjys.file.local-path:./storage}") String localPath) {
        this.root = Path.of(localPath).toAbsolutePath().normalize();
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        try {
            Path target = root.resolve(objectKey).normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("文件对象键无效");
            Files.createDirectories(target.getParent());
            Files.write(target, content);
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件保存失败", exception);
        }
    }

    @Override
    public byte[] read(String objectKey) {
        try {
            Path target = root.resolve(objectKey).normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("文件对象键无效");
            return Files.readAllBytes(target);
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件读取失败", exception);
        }
    }

    @Override
    public void delete(String objectKey) {
        try {
            Path target = root.resolve(objectKey).normalize();
            if (!target.startsWith(root)) throw new IllegalArgumentException("文件对象键无效");
            Files.deleteIfExists(target);
        } catch (IOException exception) {
            throw new IllegalStateException("本地文件删除失败", exception);
        }
    }
}
