package com.wjys.support.file;

import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.io.ByteArrayInputStream;

@Service
@ConditionalOnProperty(name = "wjys.file.minio-enabled", havingValue = "true")
public class MinioObjectStorageService implements ObjectStorageService {
    private final MinioClient minioClient;
    private final String bucket;

    public MinioObjectStorageService(
            @Value("${wjys.file.endpoint}") String endpoint,
            @Value("${wjys.file.access-key}") String accessKey,
            @Value("${wjys.file.secret-key}") String secretKey,
            @Value("${wjys.file.bucket:wjys}") String bucket
    ) {
        this.minioClient = MinioClient.builder().endpoint(endpoint).credentials(accessKey, secretKey).build();
        this.bucket = bucket;
    }

    @PostConstruct
    void ensureBucket() {
        try {
            if (!minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
            }
        } catch (Exception exception) {
            throw new IllegalStateException("MinIO 存储桶初始化失败", exception);
        }
    }

    @Override
    public void store(String objectKey, byte[] content, String contentType) {
        try {
            minioClient.putObject(PutObjectArgs.builder().bucket(bucket).object(objectKey).stream(new ByteArrayInputStream(content), content.length, -1).contentType(contentType).build());
        } catch (Exception exception) { throw new IllegalStateException("MinIO 文件保存失败", exception); }
    }

    @Override
    public byte[] read(String objectKey) {
        try (var stream = minioClient.getObject(GetObjectArgs.builder().bucket(bucket).object(objectKey).build())) { return stream.readAllBytes(); }
        catch (Exception exception) { throw new IllegalStateException("MinIO 文件读取失败", exception); }
    }

    @Override
    public void delete(String objectKey) {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build());
        } catch (Exception exception) { throw new IllegalStateException("MinIO 文件删除失败", exception); }
    }
}
